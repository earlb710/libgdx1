package eb.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import static java.lang.Math.abs;
import java.nio.ByteBuffer;

/**
 *
 * @author Earl Bosch
 */
public class UtilLibGDX {

    public static Pixmap createPixmapTileLayout(int tileWidth, int tileHeight, int cols, int rows, Pixmap[] pix) {
        Pixmap ret = new Pixmap(tileWidth * cols, tileHeight * rows, Pixmap.Format.RGBA8888);
        int x = 0, y = 0, idx = 0;
        boolean done = false;
        while (idx < pix.length && !done) {
            Pixmap cpix = pix[idx];
            if (cpix != null) {
                int swidth = cpix.getWidth();
                int sheight = cpix.getHeight();
                ret.drawPixmap(cpix, 0, 0, swidth, sheight, x * tileWidth, y * tileHeight, tileWidth, tileHeight);
            }
            idx = idx + 1;
            x = x + 1;
            if (x >= cols) {
                x = 0;
                y = y + 1;
                if (y >= rows) {
                    done = true;
                }
            }
        }
        return ret;
    }

    public static Pixmap newPixmapColor(int width, int height, Color color) {
        return newPixmapColor(width, height, color, (byte) (255 * color.a));
    }

    public static Pixmap newPixmapColor(int width, int height, Color color, int alpha) {
        return newPixmapColor(width, height, color, (byte) alpha);
    }

    public static Pixmap newPixmapColor(int width, int height, Color color, byte alpha) {
        Pixmap newSource = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        ByteBuffer buffer = newSource.getPixels();
        int length = buffer.limit();
        int c = color.toIntBits();
        byte r = (byte) ((c) & 0xFF);
        byte g = (byte) ((c >> 8) & 0xFF);
        byte b = (byte) ((c >> 16) & 0xFF);
        byte a = alpha;
        for (int idx = 0; idx < length; idx = idx + 4) {
            buffer.put(idx, r);
            buffer.put(idx + 1, g);
            buffer.put(idx + 2, b);
            buffer.put(idx + 3, a);
        }
        buffer.position(0);
        return newSource;
    }

    public static Pixmap newPixmapColor(int width, int height, byte[] color) {
        Pixmap newSource = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        ByteBuffer buffer = newSource.getPixels();
        int length = buffer.limit();
        byte r = (byte) ((color[0]) & 0xFF);
        byte g = (byte) ((color[1]) & 0xFF);
        byte b = (byte) ((color[2]) & 0xFF);
        byte a = (byte) 255;
        if (color.length == 4) {
            a = color[3];
        }
        for (int idx = 0; idx < length; idx = idx + 4) {
            buffer.put(idx, r);
            buffer.put(idx + 1, g);
            buffer.put(idx + 2, b);
            buffer.put(idx + 3, a);
        }
        buffer.position(0);
        return newSource;
    }

    public static Pixmap newPixmapAlpha(int width, int height, int alpha) {
        return newPixmapAlpha(width, height, (byte) alpha);
    }

    public static Pixmap newPixmapAlpha(int width, int height, byte alpha) {
        Pixmap newSource = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        ByteBuffer buffer = newSource.getPixels();
        int length = buffer.limit();
        for (int idx = 0; idx < length; idx = idx + 4) {
            buffer.put(idx + 3, alpha);
        }
        buffer.position(0);
        return newSource;
    }

    public static Pixmap newPixmapAlpha(Pixmap source, float alpha) {
        Pixmap newSource = new Pixmap(source.getWidth(), source.getHeight(), Pixmap.Format.RGBA8888);
        newSource.drawPixmap(source, 0, 0);
        ByteBuffer buffer = newSource.getPixels();
        int length = buffer.limit();
        for (int idx = 0; idx < length; idx = idx + 4) {
            int a = buffer.get(idx + 3) & 0xFF;
            buffer.put(idx + 3, (byte) (a * alpha));
        }
        buffer.position(0);
        return newSource;
    }

    public static Pixmap newPixmapGrayscale(Pixmap source, int alpha) {
        return newPixmapGrayscale(source, (byte) alpha);
    }

    public static Pixmap newPixmapGrayscale(Pixmap source) {
        return newPixmapGrayscale(source, (byte) 255);
    }

    public static Pixmap newPixmapGrayscale(Pixmap source, byte alpha) {
        Pixmap newSource = new Pixmap(source.getWidth(), source.getHeight(), Pixmap.Format.RGBA8888);
        newSource.drawPixmap(source, 0, 0);
        ByteBuffer buffer = newSource.getPixels();
        int length = buffer.limit();
        for (int idx = 0; idx < length; idx = idx + 4) {
            byte r = buffer.get(idx);
            byte g = buffer.get(idx + 1);
            byte b = buffer.get(idx + 2);
            byte c = (byte) ((int) (((r & 0xFF) + ((g & 0xFF) * 1.2) + ((b & 0xFF) * 0.8)) / 3.0));
            buffer.put(idx, c);
            buffer.put(idx + 1, c);
            buffer.put(idx + 2, c);
            buffer.put(idx + 3, alpha);
        }
        buffer.position(0);
        return newSource;
    }

    public static byte getPixmapAlpha(Pixmap source) {
        ByteBuffer buffer = source.getPixels();
        return buffer.get(3);
    }

    public static Pixmap createPixmapFlipV(Pixmap source) {
        int w = source.getWidth();
        int h = source.getHeight();
        Pixmap dest = new Pixmap(w, h, source.getFormat());
        ByteBuffer bufSource = source.getPixels();
        ByteBuffer bufDest = dest.getPixels();
        int length = bufSource.limit();
        int bcount = length / (w * h);
        int bline = bcount * w;
        for (int idx = 0; idx < h; idx = idx + 1) {
            bufDest.put(idx * bline, bufSource, length - ((idx + 1) * bline), bline);
        }
        bufDest.position(0);
        return dest;
    }

    public static Pixmap createPixmapFlipH(Pixmap source) {
        int w = source.getWidth();
        int h = source.getHeight();
        Pixmap dest = new Pixmap(w, h, source.getFormat());
        ByteBuffer bufSource = source.getPixels();
        ByteBuffer bufDest = dest.getPixels();
        int length = bufSource.limit();
        int bcount = length / (w * h);
        int bline = bcount * w;
        byte[] bbuf = new byte[bcount];
        for (int idy = 0; idy < h; idy = idy + 1) {
            for (int idx = 0; idx < w; idx = idx + 1) {
                int sx = w - idx - 1;
                bufSource.get(idy * bline + sx * bcount, bbuf, 0, bcount);
                bufDest.put(idy * bline + idx * bcount, bbuf);
            }
        }
        bufDest.position(0);
        return dest;
    }

    public static Pixmap createPixmapRotate(Pixmap source, int rcount) {
        rcount = rcount % 4;
        int w = source.getWidth();
        int h = source.getHeight();
        Pixmap dest = (rcount == 2) ? new Pixmap(w, h, source.getFormat()) : new Pixmap(h, w, source.getFormat());
        ByteBuffer bufSource = source.getPixels();
        ByteBuffer bufDest = dest.getPixels();
        int length = bufSource.limit();
        int bcount = length / (w * h);
        int blineSrc = bcount * w;
        int blineDst = (rcount == 2) ? bcount * w : bcount * h;
        byte[] bbuf = new byte[bcount];
        switch (rcount) {
            case 1:
                for (int idy = 0; idy < h; idy = idy + 1) {
                    for (int idx = 0; idx < w; idx = idx + 1) {
                        int dy = h - idy - 1;
                        bufSource.get(idy * blineSrc + idx * bcount, bbuf, 0, bcount);
                        bufDest.put(idx * blineDst + dy * bcount, bbuf);
                    }
                }
                break;
            case 2:
                for (int idy = 0; idy < h; idy = idy + 1) {
                    for (int idx = 0; idx < w; idx = idx + 1) {
                        int dy = h - idy - 1;
                        int dx = w - idx - 1;
                        bufSource.get(idy * blineSrc + idx * bcount, bbuf, 0, bcount);
                        bufDest.put(dy * blineDst + dx * bcount, bbuf);
                    }
                }
                break;
            case 3:
                for (int idy = 0; idy < h; idy = idy + 1) {
                    for (int idx = 0; idx < w; idx = idx + 1) {
                        int dx = w - idx - 1;
                        bufSource.get(idy * blineSrc + idx * bcount, bbuf, 0, bcount);
                        bufDest.put(dx * blineDst + idy * bcount, bbuf);
                    }
                }
                break;
        }
        bufDest.position(0);
        return dest;
    }

    public static Pixmap newPixmapTint(Pixmap source, byte r, byte g, byte b, byte a) {
        return newPixmapTint(source, r & 0xFF, g & 0xFF, b & 0xFF, a & 0xFF);
    }

    public static Pixmap newPixmapTint(Pixmap source, int r, int g, int b, int a) {
        int defAlpha = 255;
        if (source.getFormat() == Format.RGBA8888 || source.getFormat() == Format.RGBA4444) {
            defAlpha = -1;
        }
        Pixmap newSource = new Pixmap(source.getWidth(), source.getHeight(), Format.RGBA8888);
        newSource.drawPixmap(source, 0, 0);
        ByteBuffer buffer = newSource.getPixels();
        int length = buffer.limit();
        for (int idx = 0; idx < length; idx = idx + 4) {
            byte pr = (byte) (((buffer.get(idx) & 0xFF) + r) / 2);
            byte pg = (byte) (((buffer.get(idx + 1) & 0xFF) + g) / 2);
            byte pb = (byte) (((buffer.get(idx + 2) & 0xFF) + b) / 2);
            byte pa = buffer.get(idx + 3);
            if (defAlpha > 0) {
                pa = (byte) ((defAlpha + a) / 2);
            } else {
                pa = (byte) (((pa & 0xFF) + a) / 2);
            }
            buffer.put(idx, pr);
            buffer.put(idx + 1, pg);
            buffer.put(idx + 2, pb);
            buffer.put(idx + 3, pa);
        }
        buffer.position(0);
        return newSource;
    }

    public static Pixmap newPixmapSetAlphaColor(Pixmap source, byte r, byte g, byte b) {
        return newPixmapSetAlphaColor(source, r & 0xFF, g & 0xFF, b & 0xFF);
    }

    public static Pixmap newPixmapSetAlphaColor(Pixmap source, int r, int g, int b) {
        Pixmap newSource = new Pixmap(source.getWidth(), source.getHeight(), Format.RGBA8888);
        newSource.drawPixmap(source, 0, 0);
        ByteBuffer buffer = newSource.getPixels();
        int length = buffer.limit();
        for (int idx = 0; idx < length; idx = idx + 4) {
            int pr = buffer.get(idx) & 0xFF;
            int pg = buffer.get(idx + 1) & 0xFF;
            int pb = buffer.get(idx + 2) & 0xFF;
            int a = (abs(pr - r) + abs(pg - g) + abs(pb - b)) / 3;
            buffer.put(idx + 3, (byte) a);
        }
        buffer.position(0);
        return newSource;
    }

    public static Pixmap newPixmapWhiteTransparent(Pixmap source) {
        return newPixmapSetAlphaColor(source, 255, 255, 255);
    }

    public static Pixmap newPixmapNegative(Pixmap source) {
        Pixmap newSource = new Pixmap(source.getWidth(), source.getHeight(), Format.RGBA8888);
        newSource.drawPixmap(source, 0, 0);
        ByteBuffer buffer = newSource.getPixels();
        int length = buffer.limit();
        for (int idx = 0; idx < length; idx = idx + 4) {
            buffer.put(idx,     (byte) (255 - (buffer.get(idx)     & 0xFF)));
            buffer.put(idx + 1, (byte) (255 - (buffer.get(idx + 1) & 0xFF)));
            buffer.put(idx + 2, (byte) (255 - (buffer.get(idx + 2) & 0xFF)));
        }
        buffer.position(0);
        return newSource;
    }

    public static Pixmap createPixmapTileLayout(int tileWidth, int tileHeight, int cols, int rows, Texture[] tex) {
        Pixmap ret = new Pixmap(tileWidth * cols, tileHeight * rows, Pixmap.Format.RGBA8888);
        int x = 0, y = 0, idx = 0;
        boolean done = false;
        while (idx < tex.length && !done) {
            Texture ctex = tex[idx];
            if (ctex != null) {
                Pixmap cpix = getPixmap(ctex);
                int swidth = cpix.getWidth();
                int sheight = cpix.getHeight();
                ret.drawPixmap(cpix, 0, 0, swidth, sheight, x * tileWidth, y * tileHeight, tileWidth, tileHeight);
            }
            idx = idx + 1;
            x = x + 1;
            if (x >= cols) {
                x = 0;
                y = y + 1;
                if (y >= rows) {
                    done = true;
                }
            }
        }
        return ret;
    }

    public static Pixmap createPixmapTileLayout(int tileWidth, int tileHeight, int cols, int rows, TextureRegion[] tex) {
        Pixmap ret = new Pixmap(tileWidth * cols, tileHeight * rows, Pixmap.Format.RGBA8888);
        int x = 0, y = 0, idx = 0;
        boolean done = false;
        while (idx < tex.length && !done) {
            TextureRegion texr = tex[idx];
            if (texr != null) {
                Pixmap cpix = getPixmap(texr.getTexture());
                ret.drawPixmap(cpix, texr.getRegionX(), texr.getRegionY(), texr.getRegionWidth(), texr.getRegionHeight(), x * tileWidth, y * tileHeight, tileWidth, tileHeight);
            }
            idx = idx + 1;
            x = x + 1;
            if (x >= cols) {
                x = 0;
                y = y + 1;
                if (y >= rows) {
                    done = true;
                }
            }
        }
        return ret;
    }

    public static Pixmap createSubPixmap(int x, int y, int width, int height, Texture source) {
        Pixmap sourcePix = convertToPixmap(source);
        Pixmap ret = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        ret.drawPixmap(sourcePix, x, y, width, height, 0, 0, width, height);
        return ret;
    }

    public static Pixmap createSubPixmap(int width, int height, TextureRegion source) {
        Texture tex = source.getTexture();
        Pixmap sourcePix = convertToPixmap(tex);
        Pixmap ret = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        ret.drawPixmap(sourcePix, source.getRegionX(), source.getRegionY(), source.getRegionWidth(), source.getRegionHeight(), 0, 0, width, height);
        return ret;
    }

    public static Pixmap createSubPixmap(int x, int y, int width, int height, Pixmap source) {
        Pixmap ret = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        ret.drawPixmap(source, x, y, width, height, 0, 0, width, height);
        return ret;
    }

    public static Texture createSubTexture(int x, int y, int width, int height, Texture source) {
        Pixmap retPix = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        Pixmap sourcePix = convertToPixmap(source);
        retPix.drawPixmap(sourcePix, x, y, width, height, 0, 0, width, height);
        Texture retTex = new Texture(retPix);
        retPix.dispose();
        return retTex;
    }

    public static Pixmap convertToPixmap(Texture tex) {
        if (!tex.getTextureData().isPrepared()) {
            tex.getTextureData().prepare();
        }
        Pixmap ret = tex.getTextureData().consumePixmap();
        return ret;
    }

    public static Texture convertToTexture(Pixmap pix) {
        Texture ret = new Texture(pix);
        return ret;
    }

    public static Pixmap getPixmap(Texture tex) {
        if (!tex.getTextureData().isPrepared()) {
            tex.getTextureData().prepare();
        }
        Pixmap ret = tex.getTextureData().consumePixmap();
        return ret;
    }

    public static Pixmap getPixmap(TextureRegion texr) {
        Texture tex = texr.getTexture();
        return getPixmap(tex);
    }

    public static Texture copyToTexture(Pixmap pixmap) {
        Texture ret = new Texture(pixmap);
        return ret;
    }

}
