package de.th.nuernberg.bme.lidlsplit;

public class DebtPair {
    private final long debtorId;
    private final long creditorId;

    public DebtPair(long debtorId, long creditorId) {
        this.debtorId = debtorId;
        this.creditorId = creditorId;
    }

    public long getDebtorId() { return debtorId; }
    public long getCreditorId() { return creditorId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DebtPair other = (DebtPair) o;
        return debtorId == other.debtorId && creditorId == other.creditorId;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(debtorId, creditorId);
    }
}
