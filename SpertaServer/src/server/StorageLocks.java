/**
 * Locks partilhados para coordenar acesso concorrente a estruturas persistentes
 * do servidor.
 */
public final class StorageLocks {
    private StorageLocks() {
    }

    public static final Object DATA_LOCK = new Object();
}
