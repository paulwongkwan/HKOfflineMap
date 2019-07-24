package mememe.omaphk.Layer;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;

/**
 * Created by Paul Wong on 17/04/25.
 */

public class GPSMarker extends Layer {
    private Bitmap bitmap;
    private int horizontalOffset;
    private LatLong latLong;
    private int verticalOffset;
    private int bearing = 0;

    /**
     * @param latLong          the initial geographical coordinates of this marker (may be null).
     * @param bitmap           the initial {@code Bitmap} of this marker (may be null).
     * @param horizontalOffset the horizontal marker offset.
     * @param verticalOffset   the vertical marker offset.
     */
    public GPSMarker(LatLong latLong, Bitmap bitmap, int horizontalOffset, int verticalOffset) {
        super();

        this.latLong = latLong;
        this.bitmap = bitmap;
        this.horizontalOffset = horizontalOffset;
        this.verticalOffset = verticalOffset;
    }

    public synchronized boolean contains(Point center, Point point) {
        Rectangle r = new Rectangle(center.x - (float) bitmap.getWidth() / 2 + this.horizontalOffset, center.y
                - (float) bitmap.getHeight() / 2 + this.verticalOffset, center.x + (float) bitmap.getWidth() / 2
                + this.horizontalOffset, center.y + (float) bitmap.getHeight() / 2 + this.verticalOffset);
        return r.contains(point);
    }

    @Override
    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (this.latLong == null || this.bitmap == null || this.bitmap.isDestroyed()) {
            return;
        }

        long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
        double pixelX = MercatorProjection.longitudeToPixelX(this.latLong.longitude, mapSize);
        double pixelY = MercatorProjection.latitudeToPixelY(this.latLong.latitude, mapSize);

        int halfBitmapWidth = this.bitmap.getWidth() / 2;
        int halfBitmapHeight = this.bitmap.getHeight() / 2;

        int left = (int) (pixelX - topLeftPoint.x - halfBitmapWidth + this.horizontalOffset);
        int top = (int) (pixelY - topLeftPoint.y - halfBitmapHeight + this.verticalOffset);
        int right = left + this.bitmap.getWidth();
        int bottom = top + this.bitmap.getHeight();

        Rectangle bitmapRectangle = new Rectangle(left, top, right, bottom);
        Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!canvasRectangle.intersects(bitmapRectangle)) {
            return;
        }

        // Rotate the marker based on bearing for example (or whatever angle you want)
        android.graphics.Canvas androidCanvas = AndroidGraphicFactory.getCanvas(canvas);
        androidCanvas.save();

        float px = (right + left) / 2f;
        float py = (bottom + top) / 2f;

        androidCanvas.rotate(bearing, px, py);
        canvas.drawBitmap(bitmap, left, top);
        androidCanvas.restore();

//        canvas.drawBitmap(this.bitmap, left, top);
    }

    public synchronized void setBearing(int bearing) {
        this.bearing = bearing;
        requestRedraw();
    }

    /**
     * @return the {@code Bitmap} of this marker (may be null).
     */
    public synchronized Bitmap getBitmap() {
        return this.bitmap;
    }

    /**
     * @return the horizontal offset of this marker.
     */
    public synchronized int getHorizontalOffset() {
        return this.horizontalOffset;
    }

    /**
     * @return the geographical coordinates of this marker (may be null).
     */
    public synchronized LatLong getLatLong() {
        return this.latLong;
    }

    /**
     * @return Gets the LatLong Position of the Object
     */
    @Override
    public synchronized LatLong getPosition() {
        return this.latLong;
    }

    /**
     * @return the vertical offset of this marker.
     */
    public synchronized int getVerticalOffset() {
        return this.verticalOffset;
    }

    @Override
    public synchronized void onDestroy() {
        if (this.bitmap != null) {
            this.bitmap.decrementRefCount();
        }
    }

    public synchronized boolean getVisible(){return getVisible();}

    /**
     * @param bitmap the new {@code Bitmap} of this marker (may be null).
     */
    public synchronized void setBitmap(Bitmap bitmap) {
        if (this.bitmap != null && this.bitmap.equals(bitmap)) {
            return;
        }
        if (this.bitmap != null) {
            this.bitmap.decrementRefCount();
        }
        this.bitmap = bitmap;
        requestRedraw();
    }

    /**
     * @param horizontalOffset the new horizontal offset of this marker.
     */
    public synchronized void setHorizontalOffset(int horizontalOffset) {
        this.horizontalOffset = horizontalOffset;
    }

    /**
     * @param latLong the new geographical coordinates of this marker (may be null).
     */
    public synchronized void setLatLong(LatLong latLong) {
        this.latLong = latLong;
        requestRedraw();
    }

    /**
     * @param verticalOffset the new vertical offset of this marker.
     */
    public synchronized void setVerticalOffset(int verticalOffset) {
        this.verticalOffset = verticalOffset;
    }
}