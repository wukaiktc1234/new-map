package com.foodtraceability.service.pos;

/** 异常单处理结果（含 replayed 幂等标识，Owner 明确要求）。 */
public class ResolveResult {

    public enum Status { OK, REPLAY, AMOUNT_MISMATCH, ALREADY_HANDLED, ALREADY_PAID, NO_ACCOUNT, ERROR }

    private final Status status;
    private final String message;
    private final boolean replayed;

    private ResolveResult(Status status, String message, boolean replayed) {
        this.status = status;
        this.message = message;
        this.replayed = replayed;
    }

    public static ResolveResult ok(String msg) { return new ResolveResult(Status.OK, msg, false); }
    public static ResolveResult replay(String msg) { return new ResolveResult(Status.REPLAY, msg, true); }
    public static ResolveResult fail(Status st, String msg) { return new ResolveResult(st, msg, false); }

    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public boolean isReplayed() { return replayed; }
}
