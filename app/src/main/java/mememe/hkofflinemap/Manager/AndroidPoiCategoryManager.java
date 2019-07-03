package mememe.hkofflinemap.Manager;

import android.database.Cursor;

import org.mapsforge.poi.storage.AbstractPoiCategoryManager;
import org.mapsforge.poi.storage.DoubleLinkedPoiCategory;
import org.mapsforge.poi.storage.PoiCategory;
import org.mapsforge.poi.storage.UnknownPoiCategoryException;
import org.sqlite.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AndroidPoiCategoryManager extends AbstractPoiCategoryManager {
    private static final Logger LOGGER = Logger.getLogger(AndroidPoiCategoryManager.class.getName());

    /**
     * @param db SQLite database object. (Using SQLite wrapper for Android).
     */
    AndroidPoiCategoryManager(SQLiteDatabase db) {
        this.categoryMap = new TreeMap<>();

        try {
            loadCategories(db);
        } catch (UnknownPoiCategoryException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Load categories from database.
     *
     * @throws UnknownPoiCategoryException if a category cannot be retrieved by its ID or unique name.
     */
    private void loadCategories(SQLiteDatabase db) throws UnknownPoiCategoryException {
        // Maximum ID (for root node)
        int maxID = 0;

        // Maps categories to their parent IDs
        Map<PoiCategory, Integer> parentMap = new HashMap<>();

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SELECT_STATEMENT, null);
            while (cursor.moveToNext()) {
                // Column values
                int categoryID = cursor.getInt(0);
                String categoryTitle = cursor.getString(1);
                int categoryParentID = cursor.getInt(2);

                PoiCategory pc = new DoubleLinkedPoiCategory(categoryTitle, null, categoryID);
                this.categoryMap.put(categoryID, pc);

                // category --> parent ID
                parentMap.put(pc, categoryParentID);

                // check for root node
                if (categoryID > maxID) {
                    maxID = categoryID;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        // Set root category and remove it from parents map
        this.rootCategory = getPoiCategoryByID(maxID);
        parentMap.remove(this.rootCategory);

        // Assign parent categories
        for (PoiCategory c : parentMap.keySet()) {
            c.setParent(getPoiCategoryByID(parentMap.get(c)));
        }
    }
}
