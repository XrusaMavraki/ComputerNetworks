package dto;

import Util.Pair;
import Util.PeerCommunicationInformation;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Created by xrusa on 19/4/2017.
 */
public class SearchResponse implements Serializable {

    public enum SearchResponseType {
        SUCCESS_SEARCH, FAIL_SEARCH
    }
    private final SearchResponseType searchType;
    private final Collection<PeerCommunicationInformation> usersWithFile;

    public SearchResponse(SearchResponseType searchType, Collection<PeerCommunicationInformation> usersWithFile){
        this.searchType=searchType;
        this.usersWithFile=usersWithFile;
    }

    public SearchResponseType getSearchType() {
        return searchType;
    }

    public Collection<PeerCommunicationInformation> getUsersWithFile() {
        return usersWithFile;
    }
}
