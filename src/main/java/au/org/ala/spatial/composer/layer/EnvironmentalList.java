package au.org.ala.spatial.composer.layer;

import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.ListEntry;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

/**
 * @author ajay
 */
public class EnvironmentalList extends Listbox {

    private static Logger logger = Logger.getLogger(EnvironmentalList.class);
    ArrayList<ListEntry> listEntries;
    float[] threasholds = {0.1f, 0.3f, 1.0f};
    SimpleListModel listModel;
    MapComposer mapComposer;
    boolean includeAnalysisLayers;
    boolean disableContextualLayers;
    boolean singleDomain;

    public void init(MapComposer mc, boolean includeAnalysisLayers, boolean disableContextualLayers, boolean singleDomain) {
        mapComposer = mc;
        this.includeAnalysisLayers = includeAnalysisLayers;
        this.disableContextualLayers = disableContextualLayers;
        this.singleDomain = singleDomain;

        try {
            setupListEntries();

            setupList();

            this.setMultiple(true);

        } catch (Exception e) {
            logger.error("error with initial setip of Environmental List", e);
        }
    }

    void setupListEntries() {
        listEntries = new ArrayList<ListEntry>();
        JSONArray ja = CommonData.getLayerListJSONArray();
        for (int i = 0; i < ja.size(); i++) {
            JSONObject jo = ja.getJSONObject(i);
            listEntries.add(
                    new ListEntry(jo.getString("name"),
                            (jo.containsKey("displayname") ? jo.getString("displayname") : jo.getString("name")),
                            (jo.containsKey("classification1") ? jo.getString("classification1") : ""),
                            (jo.containsKey("classification2") ? jo.getString("classification2") : ""),
                            (jo.containsKey("type") ? jo.getString("type") : ""),
                            (jo.containsKey("domain") ? jo.getString("domain") : ""),
                            jo));
        }

        if (includeAnalysisLayers) {         //add
            for (MapLayer ml : mapComposer.getAnalysisLayers()) {
                ListEntry le = null;
                if (ml.getSubType() == LayerUtilities.ALOC) {
                    le = new ListEntry(ml.getName(), ml.getDisplayName(), "Analysis", "Classification", "Contextual", null, null);
                } else if (ml.getSubType() == LayerUtilities.MAXENT) {
                    le = new ListEntry(ml.getName(), ml.getDisplayName(), "Analysis", "Prediction", "Environmental", null, null);
                } else if (ml.getSubType() == LayerUtilities.GDM) {
                    le = new ListEntry(ml.getName(), ml.getDisplayName(), "Analysis", "GDM", "Environmental", null, null);
                } else if (ml.getSubType() == LayerUtilities.ODENSITY) {
                    le = new ListEntry(ml.getName(), ml.getDisplayName(), "Analysis", "Occurrence Density", "Environmental", null, null);
                } else if (ml.getSubType() == LayerUtilities.SRICHNESS) {
                    le = new ListEntry(ml.getName(), ml.getDisplayName(), "Analysis", "Species Richness", "Environmental", null, null);
                }
                if (le != null) {
                    listEntries.add(le);
                }
            }
        }

        java.util.Collections.sort(listEntries, new Comparator<ListEntry>() {

            @Override
            public int compare(ListEntry e1, ListEntry e2) {
                return (e1.catagory1 + " " + e1.catagory2 + " " + e1.displayname).compareTo(e2.catagory1 + " " + e2.catagory2 + " " + e2.displayname);
            }
        });
    }

    public void setupList() {
        try {
            setItemRenderer(new ListitemRenderer() {

                @Override
                public void render(Listitem li, Object data, int item_idx) {
                    String type = ((ListEntry) data).type;

                    Image imgType = new Image();
                    if (type.equalsIgnoreCase("environmental")) {
                        imgType.setSrc("/img/icon_grid-layer.png");
                    } else {
                        imgType.setSrc("/img/icon_contextual-layer.png");
                    }
                    Listcell tc = new Listcell();
                    tc.setParent(li);
                    imgType.setParent(tc);

                    Listcell n = new Listcell(((ListEntry) data).catagoryNames());
                    //n.setHflex("1");
                    n.setParent(li);
                    n = new Listcell(((ListEntry) data).displayname);
                    //n.setHflex("1");
                    n.setParent(li);

                    Listcell lc = new Listcell();
                    lc.setParent(li);
                    lc.setValue(data);

                    if (disableContextualLayers && type.equalsIgnoreCase("contextual")) {
                        li.setDisabled(true);
                    }

                    Html img = new Html(lang"<i class='icon-info-sign'></i>");

                    img.addEventListener("onClick", new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {
                            //re-toggle the checked flag (issue 572)
                            Listitem li = (Listitem) event.getTarget().getParent().getParent();
                            li.getListbox().toggleItemSelection(li);
                            EnvironmentalList el = (EnvironmentalList) li.getParent();
                            el.updateDistances();

                            String s = ((ListEntry) ((Listcell) event.getTarget().getParent()).getValue()).name;
                            String metadata = CommonData.layersServer + "/layers/view/more/" + s;
                            mapComposer.activateLink(metadata, "Metadata", false);

                        }
                    });
                    img.setParent(lc);

                    //String type = ((ListEntry) data).type;
                    if (type.equalsIgnoreCase("environmental")) {
                        float value = ((ListEntry) data).value;
                        lc = new Listcell(" ");
                        if (threasholds[0] > value) {
                            lc.setSclass("lcRed");//setStyle("background: #bb2222;");
                        } else if (threasholds[1] > value) {
                            lc.setSclass("lcYellow");//lc.setStyle("background: #ffff22;");
                        } else if (1 >= value) {
                            lc.setSclass("lcGreen");//lc.setStyle("background: #22aa22;");
                        } else {
                            lc.setSclass("lcWhite");//setStyle("background: #ffffff;");
                        }
                        lc.setParent(li);
                    }
                }

                ;
            });

            listModel = new SimpleListModel(listEntries);
            setModel(listModel);

            setMultiple(true);
        } catch (Exception e) {
            logger.debug("error setting up env list", e);
        }
    }

    @Override
    public boolean isMultiple() {
        return true;
    }

    @Override
    public void toggleItemSelection(Listitem item) {
        super.toggleItemSelection(item);
        //update minimum distances here
        this.setMultiple(true);
    }

    public void updateDistances() {
        this.setMultiple(true);

        if (listEntries == null) {
            return;
        }

        String fieldId;

        for (ListEntry le : listEntries) {
            //set to 2 for contextual and 'no association distance'
            if (le.type.equalsIgnoreCase("contextual")
                    || le.layerObject == null
                    || !le.layerObject.containsKey("fields")
                    || (fieldId = getFieldId(le.layerObject)) == null
                    || CommonData.getDistancesMap().get(fieldId) == null) {
                le.value = 2;
            } else {
                le.value = 1;
            }
        }

        for (Object o : getSelectedItems()) {
            ListEntry l = listEntries.get(((Listitem) o).getIndex());
            l.value = 0;
            String[] domain;
            if (l.type.equalsIgnoreCase("environmental")
                    && l.layerObject != null && l.layerObject.containsKey("fields")
                    && (fieldId = getFieldId(l.layerObject)) != null
                    && CommonData.getDistancesMap().get(fieldId) != null
                    && (domain = getDomain(l.layerObject)) != null) {
                for (ListEntry le : listEntries) {
                    if (le.layerObject != null && le.layerObject.containsKey("fields")
                            && (!singleDomain || isSameDomain(getDomain(le.layerObject), domain))) {
                        String fieldId2 = getFieldId(le.layerObject);

                        Double d = CommonData.getDistancesMap().get(fieldId).get(fieldId2);
                        if (d != null) {
                            le.value = (float) Math.min(le.value, d);
                        }
                    }
                }
            }
        }

        for (int i = 0; i < listEntries.size(); i++) {
            float value = listEntries.get(i).value;
            String type = listEntries.get(i).type;
            Listcell lc = (Listcell) (getItemAtIndex(i).getLastChild());
            if (type.equalsIgnoreCase("environmental")) {
                if (threasholds[0] > value) {
                    lc.setSclass("lcRed");//setStyle("background: #bb2222;");
                } else if (threasholds[1] > value) {
                    lc.setSclass("lcYellow");//lc.setStyle("background: #ffff22;");
                } else if (1 >= value) {
                    lc.setSclass("lcGreen");//lc.setStyle("background: #22aa22;");
                } else {
                    lc.setSclass("lcWhite");//lc.setStyle("background: #ffffff;");
                }
            }
        }

        forceDomain();
    }

    void forceDomain() {
        String[] firstDomain = getFirstDomain();
        String[] thisDomain;

        if (!singleDomain || firstDomain == null) {
            for (int i = 0; i < listEntries.size(); i++) {
                boolean defaultDisable = disableContextualLayers && listEntries.get(i).type.equalsIgnoreCase("contextual");
                getItemAtIndex(i).setDisabled(defaultDisable);
            }
            return;
        }

        for (int i = 0; i < listEntries.size(); i++) {
            ListEntry l = listEntries.get(i);
            if (l.layerObject != null
                    && (thisDomain = getDomain(l.layerObject)) != null) {
                boolean defaultDisable = disableContextualLayers && listEntries.get(i).type.equalsIgnoreCase("contextual");
                boolean match = false;
                for (String d1 : firstDomain) {
                    for (String d2 : thisDomain) {
                        if (d1.equalsIgnoreCase(d2)) {
                            match = true;
                        }
                    }
                }
                getItemAtIndex(i).setDisabled(defaultDisable || !match);
                if (!match && getItemAtIndex(i).isSelected()) {
                    toggleItemSelection(getItemAtIndex(i));
                }
            }
        }
    }

    String[] getFirstDomain() {
        String[] domain = null;

        for (Object o : getSelectedItems()) {
            ListEntry l = listEntries.get(((Listitem) o).getIndex());
            if (l.type.equalsIgnoreCase("environmental")
                    && l.layerObject != null) {
                domain = getDomain(l.layerObject);
                if (domain != null) {
                    break;
                }
            }
        }

        return domain;
    }

    String[] getDomain(JSONObject layerObject) {
        if (!layerObject.containsKey("domain")) {
            return null;
        }
        String[] d = layerObject.getString("domain").split(",");
        for (int i = 0; i < d.length; i++) {
            d[i] = d[i].trim();
        }
        return d;
    }

    String getFieldId(JSONObject layerObject) {
        String fieldId = null;
        try {
            JSONArray ja = (JSONArray) layerObject.get("fields");
            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = (JSONObject) ja.get(i);
                if (jo.getString("analysis").equalsIgnoreCase("true")) {
                    fieldId = jo.getString("id");
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("error getting field id from layer JSON object: " + layerObject, e);
        }
        return fieldId;
    }

    public void onSelect(Event event) {
        this.setMultiple(true);
        updateDistances();
    }

    public String[] getSelectedLayers() {
        this.setMultiple(true);

        Set selectedItems = getSelectedItems();
        String[] selected = new String[selectedItems.size()];
        int i = 0;
        logger.debug("getSelectedLayers: ");
        for (Object o : selectedItems) {
            selected[i] = listEntries.get(((Listitem) o).getIndex()).name;
            i++;
            logger.debug(listEntries.get(((Listitem) o).getIndex()).displayname + ", " + listEntries.get(((Listitem) o).getIndex()).name);
        }
        logger.debug("");
        return selected;
    }

    public void selectLayers(String[] layers) {
        this.setMultiple(true);

        String[] firstDomain = getFirstDomain();

        for (int i = 0; i < listEntries.size(); i++) {
            for (int j = 0; j < layers.length; j++) {
                if (listEntries.get(i).displayname.equalsIgnoreCase(layers[j])
                        || listEntries.get(i).name.equalsIgnoreCase(layers[j])) {
                    if (!getItemAtIndex(i).isSelected() && (!singleDomain || isSameDomain(firstDomain, getDomain(listEntries.get(i).layerObject)))) {
                        toggleItemSelection(getItemAtIndex(i));
                    }
                    break;
                }
            }
        }

        updateDistances();
    }

    @Override
    public void clearSelection() {
        updateDistances();
        super.clearSelection();
    }

    public boolean getIncludeAnalysisLayers() {
        return includeAnalysisLayers;
    }

    private boolean isSameDomain(String[] domain1, String[] domain2) {
        if (domain1 == null || domain2 == null) {
            return true;
        }

        for (String s1 : domain1) {
            for (String s2 : domain2) {
                if (s1.equalsIgnoreCase(s2)) {
                    return true;
                }
            }
        }

        return false;
    }
}