package org.cow9.minesweeper;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;

public class Counter extends Component {
    // Non-standard seven segment display number encoding
    private static final byte[] nums = {
         0x77, 0x24, 0x5d, 0x6d, 0x2e, 0x6b, 0x7b, 0x25, 0x7f, 0x6f 
    };
    private static final byte[] r = {0, (byte)0x80, (byte)0xff, (byte)0xff};
    private static final byte[] gb = {0, 0, 0, 0};
    private static final int w = 11, h = 21;
    private static final ColorModel cm = new IndexColorModel(2, 4, r, gb, gb);
    private static final SampleModel sm = new SinglePixelPackedSampleModel(
            DataBuffer.TYPE_BYTE, w, h, new int[]{0xf});
    
    private final byte[] buf = new byte[w*h];
    private final BufferedImage im = new BufferedImage(cm,
            Raster.createWritableRaster(sm, new DataBufferByte(buf, w*h),
                    new Point(0, 0)), false, null);
    
    private final int size;
    private int value;
    private boolean disabled = false;
    
    private static final int pw10(int x) {
        int r = 1;
        while (x > 0) {r *= 10; x--;}
        return r;
    }
    
    private int maxValue() { return pw10(size)-1; }
    private int minValue() { return -(pw10(size-1)-1); }
    
    public Counter(int size, int value) {
        this.size = size;
        this.value = value;
        assert size >= 1 && value > minValue() && value <= maxValue();
        setPreferredSize(new Dimension((w+2)*size, h+2));
    }

    public void setValue(int value) {
        if (value < minValue() || value > maxValue()) disabled = true;
        else this.value = value;
        repaint();
    }
    
    public void incr() { setValue(value+1); }
    public void decr() { setValue(value-1); }
    
    private void drawBox(byte v, int offset) {
        for (int y = 0; y < w; y++) {
            int half = (w - (y<<1)) >> 31;
            int n = (y<<1 > w) ? w-y-1 : y;
            for (int x = 1; x < w-1; x++) {
                int val = (half<<1) & (v>>2) | (((half^1)<<1) & (v<<1));
                buf[offset+y*w+x] = (byte)((y^x^1)&1 | val&2);
            }
            
            for (int x = 0; x < n; x++) {
                int li = offset+y*w+x;
                buf[li] = (byte)(buf[li+1]&1 | v&2);
                buf[li+1] = 0;
                
                int ri = offset+(y+1)*w-x-1;
                buf[ri] = (byte)(buf[ri-1]&1 | (v>>1)&2);
                buf[ri-1] = 0;
            }
        }
    }

    @Override public void paint(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, (w+2)*size, h+2);
        
        if (disabled) return;

        for (int i = 0, v = Math.abs(value); i < size; i++, v /= 10) {
            byte shape = (i == size-1 && value < 0) ? 8 : nums[v%10];
            drawBox(shape, 0);
            drawBox((byte)(shape>>3), (w-1)*w);
            for (int x = 3; x < 8; x++) {
                for (int y = 3; y < 9; y++) buf[y*w+x] = 0;
                for (int y = 12; y < 18; y++) buf[y*w+x] = 0;
            }
            g.drawImage(im, (w+2)*(size-i-1)+1, 1, null);
        }
    }
}
