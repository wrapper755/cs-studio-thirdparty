package org.csstudio.archivereader.rdb;

import org.csstudio.platform.utility.rdb.RDBUtil;
import org.csstudio.platform.utility.rdb.RDBUtil.Dialect;

/** SQL statements for RDB archive access
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SQL
{
    // 'status' table
    final public String sel_stati;
    
    // 'severity' table
    final public String sel_severities;
    
    // Meta data tables
    final public String numeric_meta_sel_by_channel;
    final public String enum_sel_num_val_by_channel;

    // 'channel' table
    final public String channel_sel_by_like;
    final public String channel_sel_by_reg_exp;
    final public String channel_sel_by_name;

    // 'sample' table
    final public String sample_sel_initial_time;
    final public String sample_sel_array_vals;
    final public String sample_sel_by_id_start_end;
    
    /** Initialize SQL statements
     *  @param dialect RDB dialect
     *  @param prefix Schema (table) prefix, including "." etc. as needed
     */
    public SQL(final Dialect dialect, final String prefix)
    {
        // 'status' table
        sel_stati = "SELECT status_id, name FROM " + prefix + "status";

        // 'severity' table
        sel_severities = "SELECT severity_id, name FROM " + prefix + "severity";

        // Meta data tables
        numeric_meta_sel_by_channel = "SELECT low_disp_rng, high_disp_rng," +
        " low_warn_lmt, high_warn_lmt," +
        " low_alarm_lmt, high_alarm_lmt," +
        " prec, unit FROM " + prefix + "num_metadata WHERE channel_id=?";
        
        enum_sel_num_val_by_channel = "SELECT enum_nbr, enum_val FROM "
            + prefix + "enum_metadata WHERE channel_id=? ORDER BY enum_nbr";
        
        // 'channel' table
        if (dialect == RDBUtil.Dialect.Oracle)
        {   // '\\' because Java swallows one '\', be case-insensitive by using all lowercase
            channel_sel_by_like = "SELECT name FROM " + prefix + "channel WHERE LOWER(name) LIKE LOWER(?) ESCAPE '\\' ORDER BY name";
            // Use case-insensitive REGEXP_LIKE
            channel_sel_by_reg_exp = "SELECT name FROM " + prefix + "channel WHERE REGEXP_LIKE(name, ?, 'i') ORDER BY name";
        }
        else
        {   // MySQL uses '\' by default, and everything is  by default case-insensitive
            channel_sel_by_like = "SELECT name FROM " + prefix + "channel WHERE name LIKE ? ORDER BY name";
            channel_sel_by_reg_exp = "SELECT name FROM " + prefix + "channel WHERE name REGEXP ? ORDER BY name";
        }

        channel_sel_by_name = "SELECT channel_id FROM " + prefix + "channel WHERE name=?";
    
        // 'sample' table
        if (dialect == RDBUtil.Dialect.Oracle)
        {
            sample_sel_initial_time =
                "SELECT smpl_time FROM (SELECT smpl_time FROM " +
                prefix + "sample WHERE channel_id=? AND smpl_time<=?" +
                " ORDER BY smpl_time DESC) WHERE ROWNUM=1";
            sample_sel_by_id_start_end =
                "SELECT smpl_time, severity_id, status_id, num_val, float_val, str_val FROM " + prefix + "sample"+
                "   WHERE channel_id=?" +
                "     AND smpl_time BETWEEN ? AND ?" +
                "   ORDER BY smpl_time";
            sample_sel_array_vals = "SELECT float_val FROM " + prefix + "array_val" +
                " WHERE channel_id=? AND smpl_time=? ORDER BY seq_nbr";
        }
        else
        {
            sample_sel_initial_time =
                "SELECT smpl_time, nanosecs" +
                "   FROM " + prefix + "sample WHERE channel_id=? AND smpl_time<=?" +
                "   ORDER BY smpl_time DESC, nanosecs DESC LIMIT 1";
            sample_sel_by_id_start_end =
                "SELECT smpl_time, severity_id, status_id, num_val, float_val, str_val, nanosecs FROM " + prefix + "sample" +
                "   WHERE channel_id=?" +
                "     AND smpl_time>=? AND smpl_time<=?" +
                "   ORDER BY smpl_time, nanosecs";
            sample_sel_array_vals = "SELECT float_val FROM " + prefix + "array_val" +
                " WHERE channel_id=? AND smpl_time=? AND nanosecs=? ORDER BY seq_nbr";
        }
    }
}
