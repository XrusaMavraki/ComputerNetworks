package Util;

import java.io.Serializable;

/**
 * Class to keep information about a single file.
 */
public class FileInfo implements Serializable {
    private final String fileName;
    private final long fileSize;
    private final int numParts;

    public FileInfo(String fileName, long fileSize, int numParts) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.numParts = numParts;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getNumParts() {
        return numParts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileInfo fileInfo = (FileInfo) o;

        return fileName.equals(fileInfo.fileName);
    }

    @Override
    public int hashCode() {
        return fileName.hashCode();
    }
}
