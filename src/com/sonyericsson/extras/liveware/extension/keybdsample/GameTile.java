
package com.sonyericsson.extras.liveware.extension.keybdsample;

import android.graphics.Bitmap;

public class GameTile {
    public int correctPosition;

    public String text;

    public TilePosition tilePosition;

    public Bitmap bitmap;

    @Override
    public String toString() {
        return "GameTile " + text + "\n" + tilePosition.toString();
    }
}
