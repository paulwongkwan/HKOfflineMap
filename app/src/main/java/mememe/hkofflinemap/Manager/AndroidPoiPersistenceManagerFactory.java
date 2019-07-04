package mememe.hkofflinemap.Manager;

import org.mapsforge.poi.storage.PoiPersistenceManager;

/**
 * Factory providing methods for instantiating {@link PoiPersistenceManager} implementations.
 * This class is needed to differ between Android and AWT.
 */
public class AndroidPoiPersistenceManagerFactory {
    /**
     * @param poiFilePath Path to a .poi file.
     * @return {@link PoiPersistenceManager} using an underlying SQLite database.
     */
    public static PoiPersistenceManager getPoiPersistenceManager(String poiFilePath) {
        return getPoiPersistenceManager(poiFilePath, true);
    }

    /**
     * @param poiFilePath Path to a .poi file.
     * @param readOnly    If the file does not exist it can be created and filled.
     * @return {@link PoiPersistenceManager} using an underlying SQLite database.
     */
    public static PoiPersistenceManager getPoiPersistenceManager(String poiFilePath,
                                                                 boolean readOnly) {
        return new AndroidPoiPersistenceManager(poiFilePath, readOnly);
    }
}
