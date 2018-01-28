package se.wastedtime.steering;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapGroupLayer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import sun.rmi.runtime.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * MapRenderer to split the TiledMap into layers.
 * The layers before the player layer are added to the firstRenderLayers List
 * the layers after are added to the lastRenderLayers
 */
public class MapRenderer extends OrthogonalTiledMapRenderer {

    private List<MapLayer> firstRenderLayers = new ArrayList<>();
    private List<MapLayer> lastRenderLayers = new ArrayList<>();
    private boolean firstRun = false;
    boolean switchList = false;

    public MapRenderer(TiledMap map, float unitScale) {
        super(map, unitScale);

        for (MapLayer layer : map.getLayers()) {
            if (layer instanceof MapGroupLayer) {
                MapGroupLayer mapGroupLayer = (MapGroupLayer) layer;
                for (MapLayer mapLayer : mapGroupLayer.getLayers()) {
                    addLayer(mapLayer);
                }
            } else
                addLayer(layer);
        }
    }

    private void addLayer(MapLayer layer) {
        if (layer.isVisible()) {
            if (layer.getName().equalsIgnoreCase("player"))
                switchList = true;
            if (!switchList) {
                Gdx.app.log("map", "Adding layer '{}' to firstRenderLayers" + layer.getName());
                firstRenderLayers.add(layer);
            } else {
                Gdx.app.log("map", "Adding layer '{}' to lastRenderLayers" + layer.getName());
                lastRenderLayers.add(layer);
            }
        }
    }

    public void renderBefore() {
        beginRender();
        firstRenderLayers.forEach(l -> {
            if (l instanceof TiledMapTileLayer)
                renderTileLayer((TiledMapTileLayer) l);
            else
                l.getObjects().forEach(this::renderObject);
        });
        endRender();
        firstRun = true;
    }

    public void renderAfter() {
        if (!firstRun)
            Gdx.app.log("map","Wrong MapRenderer order");

        beginRender();
        lastRenderLayers.forEach(l -> {
            if (l instanceof TiledMapTileLayer)
                renderTileLayer((TiledMapTileLayer) l);
            else
                l.getObjects().forEach(this::renderObject);
        });
        endRender();
    }
}


