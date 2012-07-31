
package open.dolphin.util;

import open.dolphin.client.ImageEntry;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 * DicomObjectを含めたImageEntry
 *
 * @author masuda, Masuda Naika
 */

public class DicomImageEntry extends ImageEntry implements Comparable {

    private DicomObject dicomObject;

    public void setDicomObject(DicomObject object) {
        dicomObject = object;
    }

    public DicomObject getDicomObject() {
        return dicomObject;
    }

    // 編集用にリサイズした画像を保存しておく
    // KartePaneのimageEntryDroppedで参照している
    private byte[] jpegBytes;
    public byte[] getResizedJpegBytes() {
        return jpegBytes;
    }
    public void setResizedJpegBytes(byte[] bf){
        jpegBytes = bf;
    }

    @Override
    public int compareTo(Object o) {
        int seriesNo = dicomObject.getInt(Tag.SeriesNumber);
        int imageNo = dicomObject.getInt(Tag.InstanceNumber);
        int testSeriesNo = ((DicomImageEntry) o).getDicomObject().getInt(Tag.SeriesNumber);
        int testImageNo = ((DicomImageEntry) o).getDicomObject().getInt(Tag.InstanceNumber);
        if (seriesNo > testSeriesNo) {
            return 1;
        } else if (seriesNo < testSeriesNo) {
            return -1;
        } else {
            if (imageNo > testImageNo) {
                return 1;
            } else if (imageNo < testImageNo) {
                return -1;
            }
        }
        return 0;
    }
}
