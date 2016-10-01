package entity.cep;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by aushani on 6/27/16.
 */

@XmlRootElement(name="GroupList")
public class GroupList {

    List<Group> groupList;

    public List<Group> getGroupList() {
        return groupList;
    }

    @XmlElement(name="Group")
    public void setGroupList(List<Group> groupList) {
        this.groupList = groupList;
    }
}
