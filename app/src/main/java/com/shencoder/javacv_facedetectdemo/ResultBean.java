package com.shencoder.javacv_facedetectdemo;

/**
 * @author ShenBen
 * @date 2021/7/9 16:14
 * @email 714081644@qq.com
 */
public class ResultBean {

    private int ResCode;
    private String ResText;
    private Data Data;

    public void setResCode(int ResCode) {
        this.ResCode = ResCode;
    }

    public int getResCode() {
        return ResCode;
    }

    public void setResText(String ResText) {
        this.ResText = ResText;
    }

    public String getResText() {
        return ResText;
    }

    public void setData(Data Data) {
        this.Data = Data;
    }

    public Data getData() {
        return Data;
    }

    public static final class Data {

        private int ID;
        private String PrisonId;
        private int UserTypeId;
        private String UserId;
        private String UserName;
        private String FaceFeature;
        private double Similarity;

        public void setID(int ID) {
            this.ID = ID;
        }

        public int getID() {
            return ID;
        }

        public void setPrisonId(String PrisonId) {
            this.PrisonId = PrisonId;
        }

        public String getPrisonId() {
            return PrisonId;
        }

        public void setUserTypeId(int UserTypeId) {
            this.UserTypeId = UserTypeId;
        }

        public int getUserTypeId() {
            return UserTypeId;
        }

        public void setUserId(String UserId) {
            this.UserId = UserId;
        }

        public String getUserId() {
            return UserId;
        }

        public void setUserName(String UserName) {
            this.UserName = UserName;
        }

        public String getUserName() {
            return UserName;
        }

        public void setFaceFeature(String FaceFeature) {
            this.FaceFeature = FaceFeature;
        }

        public String getFaceFeature() {
            return FaceFeature;
        }

        public void setSimilarity(double Similarity) {
            this.Similarity = Similarity;
        }

        public double getSimilarity() {
            return Similarity;
        }

    }
}
