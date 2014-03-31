/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.ws;

import au.org.ala.spatial.data.FacetCache;
import au.org.emii.portal.config.ConfigurationLoaderStage1Impl;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;

/**
 * @author Adam
 */
@Controller
public class General {
    private static Logger logger = Logger.getLogger(General.class);

    final static int HIGHLIGHT_RADIUS = 3;
    @Inject
    private FacetCache facetCache;

    @RequestMapping(value = "/admin/reloadconfig", method = RequestMethod.GET)
    @ResponseBody
    public String reloadConfig() {
        //signal for reload
        ConfigurationLoaderStage1Impl.loaders.get(0).interrupt();

        //reload the facet cache
        facetCache.reloadCache();

        //return summary

        //was it successful?
        return "";
    }
}
