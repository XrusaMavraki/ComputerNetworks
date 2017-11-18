package dto;

import java.io.Serializable;

/**
 * Created by xrusa on 19/4/2017.
 */
public class SearchRequest implements Serializable{
        private final String  fileName;

        public SearchRequest(String fileName){
            this.fileName=fileName;
        }

        public String getFileName(){return fileName;}
}
