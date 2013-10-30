
package com.sonyericsson.extras.liveware.extension.keybdsample;

import android.graphics.Rect;
import android.util.Log;

public class TilePosition {
    public String position;

    public Rect frame;

    public TilePosition(int position, Rect frame) {
    	char[] ls = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    	  String r = "";
    	  while(true) {
    		  int i = position;
    		  r =String.valueOf(ls[(i * 2)-1])+String.valueOf(ls[i * 2]) + r;
    	    if(position < 26) {
    	      break;
    	    }
    	    position /= 26;
    	  }
    	  
    	Log.d("Keybd", "Keybd: "+r);
    	this.position = r;
        this.frame = frame;
    }

    @Override
    public String toString() {
        return "TilePosition " + position + ", frame " + frame.toShortString();
    }
}
