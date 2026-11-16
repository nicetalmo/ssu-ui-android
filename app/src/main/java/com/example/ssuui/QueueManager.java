package com.example.ssuui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

/*
 사용 방법

 QueueManager.getInstance().joinQueue(storeId, 인원, 휴대전화번호, new QueueManager.JoinCallback() {
    public void onSuccess(QueueManager.Ticket ticket) { ... 대기 번호(ticket.number) 보여 주기 ... }
    public void onFailure(String message) { ... 메시지 보여 주고 입력값은 그대로 두기 ... }
 });

 대기 등록 문서: stores/{storeId}/queue/{entryId}
   number     대기 번호 (stores 문서의 lastNumber + 1)
   partySize  인원
   phone      휴대전화 번호 (숫자만, 순서 알림용)
   status     "waiting" (호출·입장 처리는 사장님 대기열 관리에서 바꿈)
   createdAt  등록 시각 (서버 시간)
 등록하면서 stores 문서의 lastNumber, waitingCount 를 한 트랜잭션으로 1씩 올린다.
 */

public class QueueManager {

    public interface JoinCallback {
        void onSuccess(Ticket ticket);
        void onFailure(String message);
    }

    /** 발급된 대기표 */
    public static final class Ticket {
        public final String entryId;
        public final long number;      // 대기 번호
        public final long teamsAhead;  // 등록할 때 내 앞에 있던 대기 팀 수

        Ticket(String entryId, long number, long teamsAhead) {
            this.entryId = entryId;
            this.number = number;
            this.teamsAhead = teamsAhead;
        }
    }

    public static final int MIN_PARTY_SIZE = 1;
    public static final int MAX_PARTY_SIZE = 20;

    private static QueueManager instance;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private QueueManager() {}

    public static synchronized QueueManager getInstance() {
        if (instance == null) instance = new QueueManager();
        return instance;
    }

    // -------------------------- 대기열 참여 (손님) --------------------------
    public void joinQueue(String storeId, int partySize, String phone, JoinCallback callback) {
        String digits = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        String error = validate(partySize, digits);
        if (error != null) {
            callback.onFailure(error);
            return;
        }

        DocumentReference storeRef = db.collection("stores").document(storeId);
        // 트랜잭션이 다시 시도돼도 같은 문서에 쓰도록 id 는 밖에서 한 번만 만든다
        DocumentReference entryRef = storeRef.collection("queue").document();

        db.runTransaction(tx -> {
                    DocumentSnapshot store = tx.get(storeRef);
                    if (!store.exists()) return JoinResult.rejected("매장 정보를 찾지 못했어요.");

                    long waiting = longOf(store, "waitingCount", 0);
                    String blocked = blockReason(store.getString("status"), store.getString("pauseReason"),
                            waiting, longOf(store, "maxTeams", Long.MAX_VALUE));
                    if (blocked != null) return JoinResult.rejected(blocked);

                    long number = longOf(store, "lastNumber", 0) + 1;
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("number", number);
                    entry.put("partySize", partySize);
                    entry.put("phone", digits);
                    entry.put("status", "waiting");
                    entry.put("createdAt", FieldValue.serverTimestamp());

                    tx.update(storeRef, "lastNumber", number, "waitingCount", waiting + 1);
                    tx.set(entryRef, entry);
                    return JoinResult.joined(new Ticket(entryRef.getId(), number, waiting));
                })
                .addOnSuccessListener(result -> {
                    if (result.ticket != null) callback.onSuccess(result.ticket);
                    else callback.onFailure(result.rejection);
                })
                .addOnFailureListener(e -> callback.onFailure(toMessage(e)));
    }

    /**
     * 지금 대기 등록을 받을 수 없는 이유, 받을 수 있으면 null.
     * 대기 등록 화면도 같은 기준으로 버튼을 막고 이 문구를 보여 준다 (등록할 때 트랜잭션에서 한 번 더 확인).
     */
    @Nullable
    public static String blockReason(@Nullable String status, @Nullable String pauseReason,
                                     long waitingCount, long maxTeams) {
        if ("paused".equals(status)) {
            return pauseReason == null
                    ? "지금은 대기 접수를 잠시 멈췄어요."
                    : "지금은 대기 접수를 잠시 멈췄어요. (" + pauseReason + ")";
        }
        if (!"open".equals(status)) return "지금은 대기 접수를 받지 않아요.";
        if (waitingCount >= maxTeams) return "대기 팀이 가득 찼어요. 잠시 후 다시 시도해 주세요.";
        return null;
    }

    private String validate(int partySize, String digits) {
        if (partySize < MIN_PARTY_SIZE || partySize > MAX_PARTY_SIZE)
            return "인원은 " + MIN_PARTY_SIZE + "~" + MAX_PARTY_SIZE + "명으로 정해 주세요.";
        if (!digits.matches("01[0-9]{8,9}"))
            return "휴대전화 번호를 확인해 주세요.";
        return null;
    }

    private static long longOf(DocumentSnapshot snap, String field, long fallback) {
        Long value = snap.getLong(field);
        return value == null ? fallback : value;
    }

    private String toMessage(Exception e) {
        if (e instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.UNAVAILABLE)
            return "인터넷 연결을 확인해 주세요.";
        return "대기 등록을 하지 못했어요. 잠시 후 다시 시도해 주세요.";
    }

    /** 트랜잭션 결과: 대기표 또는 등록을 받지 않은 이유 (받지 않을 때는 아무것도 쓰지 않음) */
    private static final class JoinResult {
        @Nullable final Ticket ticket;
        @Nullable final String rejection;

        private JoinResult(@Nullable Ticket ticket, @Nullable String rejection) {
            this.ticket = ticket;
            this.rejection = rejection;
        }

        static JoinResult joined(@NonNull Ticket ticket) {
            return new JoinResult(ticket, null);
        }

        static JoinResult rejected(@NonNull String rejection) {
            return new JoinResult(null, rejection);
        }
    }
}
