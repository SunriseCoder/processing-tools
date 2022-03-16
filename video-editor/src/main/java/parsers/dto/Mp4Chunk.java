package parsers.dto;

public class Mp4Chunk {
    private long addressInFile;
    private long dataSize;
    private String type;

    public long getAddressInFile() {
        return addressInFile;
    }

    public void setAddressInFile(long addressInFile) {
        this.addressInFile = addressInFile;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("Chunk[")
                .append("addressInFile=").append(addressInFile)
                .append(", dataSize=").append(dataSize)
                .append(", type=").append(type)
                .append("]").toString();
    }
}