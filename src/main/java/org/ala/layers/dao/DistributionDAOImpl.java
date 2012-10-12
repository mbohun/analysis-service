/**************************************************************************
 *  Copyright (C) 2012 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package org.ala.layers.dao;

import java.util.*;

import javax.annotation.Resource;
import javax.sql.DataSource;
import org.ala.layers.dto.Distribution;
import org.ala.layers.intersect.IntersectConfig;
import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedBeanPropertyRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * @author ajay
 */
@Service("distributionDao")
public class DistributionDAOImpl implements DistributionDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(DistributionDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;
    private String viewName = "distributions";
    private final String SELECT_CLAUSE = "select gid,spcode,scientific,authority_,common_nam,\"family\",genus_name,specific_n,min_depth,"
            + "max_depth,pelagic_fl,coastal_fl,desmersal_fl,estuarine_fl,family_lsid,genus_lsid,caab_species_number,"
            + "caab_family_number,group_name,metadata_u,wmsurl,lsid,type,area_name,pid,checklist_name,area_km, notes, geom_idx";

    @Override
    public List<Distribution> queryDistributions(String wkt, double min_depth, double max_depth, Integer geomIdx, String lsids, String type) {
        return queryDistributions(wkt, min_depth, max_depth, null, null, null, null, null, geomIdx, lsids, null, null, null, null, type);
    }

    @Override
    public List<Distribution> queryDistributions(String wkt, double min_depth, double max_depth, Boolean pelagic, Boolean coastal, Boolean estuarine, Boolean desmersal, String groupName,
            Integer geomIdx, String lsids, String[] families, String[] familyLsids, String[] genera, String[] generaLsids, String type) {
        logger.info("Getting distributions list");

        StringBuilder whereClause = new StringBuilder();
        Map<String, Object> params = new HashMap<String, Object>();
        constructWhereClause(min_depth, max_depth, pelagic, coastal, estuarine, desmersal, groupName, geomIdx, lsids, families, familyLsids, genera, generaLsids, type, params, whereClause);
        if (wkt != null && wkt.length() > 0) {
            if (whereClause.length() > 0) {
                whereClause.append(" AND ");
            }
            whereClause.append("ST_INTERSECTS(the_geom, ST_GEOMFROMTEXT( :wkt , 4326))");
            params.put("wkt", wkt);
        }

        String sql = SELECT_CLAUSE + " from " + viewName;
        if (whereClause.length() > 0) {
            sql += " WHERE " + whereClause.toString();
        }

        return updateWMSUrl(jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params));
    }

    @Override
    public Distribution getDistributionBySpcode(long spcode, String type) {
        String sql = "select gid,spcode,scientific,authority_,common_nam,\"family\",genus_name,specific_n,min_depth,max_depth,pelagic_fl,metadata_u,wmsurl,lsid,type,area_name,pid, ST_AsText(the_geom) as geometry, checklist_name, area_km, notes, geom_idx from distributions where spcode= ? and type= ? ";
        List<Distribution> d = updateWMSUrl(jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), (double) spcode, type));
        if (d.size() > 0) {
            return d.get(0);
        }
        return null;
    }

    public List<Distribution> queryDistributionsByRadius(float longitude, float latitude, float radiusInMetres, double min_depth, double max_depth, Integer geomIdx, String lsids, String[] families,
            String[] familyLsids, String[] genera, String[] generaLsids, String type) {
        return queryDistributionsByRadius(longitude, latitude, radiusInMetres, min_depth, max_depth, null, null, null, null, null, geomIdx, lsids, families, familyLsids, genera, generaLsids, type);
    }

    /**
     * Query by radius
     * 
     * @return set of species with distributions intersecting the radius
     */
    public List<Distribution> queryDistributionsByRadius(float longitude, float latitude, float radiusInMetres, double min_depth, double max_depth, Boolean pelagic, Boolean coastal,
            Boolean estuarine, Boolean desmersal, String groupName, Integer geomIdx, String lsids, String[] families, String[] familyLsids, String[] genera, String[] generaLsids, String type) {
        logger.info("Getting distributions list with a radius");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("radius", radiusInMetres);
        params.put("type", type);
        String pointGeom = "POINT(" + longitude + " " + latitude + ")";

        String sql = SELECT_CLAUSE + " from " + viewName + " " + "where ST_Distance_Sphere(the_geom, ST_GeomFromText('" + pointGeom + "', 4326)) <= :radius";

        // add additional criteria
        StringBuilder whereClause = new StringBuilder();

        constructWhereClause(min_depth, max_depth, pelagic, coastal, estuarine, desmersal, groupName, geomIdx, lsids, families, familyLsids, genera, generaLsids, type, params, whereClause);

        if (whereClause.length() > 0) {
            sql += " AND " + whereClause.toString();
        }
        return updateWMSUrl(jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params));
    }

    @Override
    public List<Distribution> getDistributionByLSID(String[] lsids) {
        String sql = SELECT_CLAUSE + " from " + viewName + " where lsid IN (:lsids)";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("lsids", Arrays.asList(lsids));
        params.put("type", Distribution.EXPERT_DISTRIBUTION);
        return updateWMSUrl(jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Distribution.class), params));
    }

    /**
     * 
     * @param min_depth
     * @param max_depth
     * @param geomIdx
     * @param lsids
     * @param params
     * @param where
     */
    private void constructWhereClause(double min_depth, double max_depth, Boolean pelagic, Boolean coastal, Boolean estuarine, Boolean desmersal, String groupName, Integer geomIdx, String lsids,
            String[] families, String[] familyLsids, String[] genera, String[] generaLsids, String type, Map<String, Object> params, StringBuilder where) {
        if (geomIdx != null && geomIdx >= 0) {
            where.append(" geom_idx = :geom_idx ");
            params.put("geom_idx", geomIdx);
        }

        if (lsids != null && lsids.length() > 0) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(":lsids LIKE '% '||lsid||' %'  ");
            params.put("lsids", " " + lsids.replace(",", " ") + " ");
        }

        if (min_depth != -1 && max_depth != -1) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("min_depth <= :max_depth AND max_depth >= :min_depth ");
            params.put("max_depth", new Double(max_depth));
            params.put("min_depth", new Double(min_depth));
        } else if (min_depth != -1) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("max_depth >= :min_depth ");
            params.put("min_depth", new Double(min_depth));
        } else if (max_depth != -1) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("min_depth <= :max_depth ");
            params.put("max_depth", new Double(max_depth));
        }

        if (pelagic != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            if (pelagic) {
                where.append("pelagic_fl > 0 ");
            } else {
                where.append("pelagic_fl = 0 ");
            }
        }

        if (coastal != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("coastal_fl = :coastal ");
            params.put("coastal", coastal ? 1 : 0);
        }

        if (estuarine != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("estuarine_fl = :estuarine ");
            params.put("estuarine", estuarine ? 1 : 0);
        }

        if (desmersal != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("desmersal_fl = :desmersal ");
            params.put("desmersal", desmersal ? 1 : 0);
        }

        if (type != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("type = :distribution_type ");
            params.put("distribution_type", type);
        }

        if (groupName != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("group_name = :groupName ");
            params.put("groupName", groupName);
        }

        if (families != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("family IN (:families) ");
            params.put("families", Arrays.asList(families));
        }

        if (familyLsids != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("family_lsid IN (:familyLsids) ");
            params.put("familyLsids", Arrays.asList(familyLsids));
        }

        if (genera != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("genus_name IN (:genera) ");
            params.put("genera", Arrays.asList(genera));
        }

        if (generaLsids != null) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("genus_lsid IN (:generaLsids) ");
            params.put("generaLsids", Arrays.asList(generaLsids));
        }
    }

    private List<Distribution> updateWMSUrl(List<Distribution> distributions) {
        if (distributions != null) {
            for (Distribution distribution : distributions) {
                if (distribution.getWmsurl() != null) {
                    distribution.setWmsurl(distribution.getWmsurl().replace(IntersectConfig.GEOSERVER_URL_PLACEHOLDER, IntersectConfig.getGeoserverUrl()));
                }
            }
        }

        return distributions;
    }

    @Override
    @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
    public Map<String, Double> identifyOutlierPointsForDistribution(String lsid, Map<String, Map<String, Double>> points) {
        Map<String, Double> outlierDistances = new HashMap<String, Double>();

        // get the id of the shape in the distributionshapes table associated
        // with the lsid
        try {
            int expertDistributionShapeId = jdbcTemplate.queryForInt("SELECT geom_idx from distributiondata WHERE lsid = ?", lsid);

            // if no points were supplied (empty map) then just return an empty
            // result map
            if (points.isEmpty()) {
                return outlierDistances;
            }

            // Create temporary table for the point information
            jdbcTemplate.update("CREATE TEMPORARY TABLE temp_exp_dist_outliers (id text PRIMARY KEY, point geography) ON COMMIT DROP");

            // Insert all the points into the temporary table, along with the
            // uuids
            // for the points
            for (String uuid : points.keySet()) {
                Map<String, Double> pointDetails = points.get(uuid);
                if (pointDetails != null) {
                    Double latitude = pointDetails.get("decimalLatitude");
                    Double longitude = pointDetails.get("decimalLongitude");

                    if (latitude != null && longitude != null) {
                        String wkt = "POINT(" + longitude + " " + latitude + ")";
                        jdbcTemplate.update("INSERT INTO temp_exp_dist_outliers VALUES (?, ST_GeographyFromText(?))", uuid, wkt);
                    }
                }
            }

            // for points that fall outside the distribution, return the
            // distance from the distribution
            List<Map<String, Object>> outlierDistancesQueryResult = jdbcTemplate
                    .queryForList(
                            "select id, ST_DISTANCE(point, (SELECT Geography(the_geom) from distributionshapes where id = ?)) as distance from temp_exp_dist_outliers where ST_DISTANCE(point, (SELECT Geography(the_geom) from distributionshapes where id = ?)) > 0;",
                            expertDistributionShapeId, expertDistributionShapeId);

            for (Map<String, Object> queryResultRow : outlierDistancesQueryResult) {
                String uuid = (String) queryResultRow.get("id");
                Double distance = (Double) queryResultRow.get("distance");

                outlierDistances.put(uuid, distance);
            }
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("No expert distribution associated with lsid " + lsid, ex);
        }

        return outlierDistances;
    }

    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
}
