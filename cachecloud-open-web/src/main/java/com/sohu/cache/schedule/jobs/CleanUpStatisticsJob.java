package com.sohu.cache.schedule.jobs;

import com.sohu.cache.client.service.ClientReportCostDistriService;
import com.sohu.cache.client.service.ClientReportValueDistriService;
import com.sohu.cache.stats.instance.InstanceStatsCenter;
import com.sohu.cache.util.ConstUtils;

import org.apache.commons.lang.math.NumberUtils;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerContext;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by yijunzhang on 14-12-8.
 */
public class CleanUpStatisticsJob extends CacheBaseJob {
    
    private static final long serialVersionUID = 8815839394475276540L;

    private static final String CLEAN_APP_HOUR_COMMAND_STATISTICS = "delete from app_hour_command_statistics where create_time < ?";

    private static final String CLEAN_APP_MINUTE_COMMAND_STATISTICS = "delete from app_minute_command_statistics where create_time < ?";

    private static final String CLEAN_APP_HOUR_STATISTICS = "delete from app_hour_statistics where create_time < ?";

    private static final String CLEAN_APP_MINUTE_STATISTICS = "delete from app_minute_statistics where create_time < ?";

    /**
     * 清除客户端数据
     */
    private static final String CLEAN_APP_CLIENT_MINUTE_COST_TOTAL = "delete from app_client_costtime_minute_stat_total where collect_time < ?";
    
    @Override
    public void action(JobExecutionContext context) {
        if (!ConstUtils.WHETHER_SCHEDULE_CLEAN_DATA) {
            return;
        }
        try {
            SchedulerContext schedulerContext = context.getScheduler().getContext();
            ApplicationContext applicationContext = (ApplicationContext) schedulerContext.get(APPLICATION_CONTEXT_KEY);
            InstanceStatsCenter InstanceStatsCenter =  applicationContext.getBean("instanceStatsCenter", InstanceStatsCenter.class);

            try {
                InstanceStatsCenter.cleanUpStandardStats(5);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            JdbcTemplate jdbcTemplate = applicationContext.getBean("jdbcTemplate", JdbcTemplate.class);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -31);
            Date time = calendar.getTime();
            int cleanCount = jdbcTemplate.update(CLEAN_APP_HOUR_COMMAND_STATISTICS, time);
            logger.warn("clean_app_hour_command_statistics count={}", cleanCount);
            cleanCount = jdbcTemplate.update(CLEAN_APP_MINUTE_COMMAND_STATISTICS, time);
            logger.warn("clean_app_minute_command_statistics count={}", cleanCount);
            cleanCount = jdbcTemplate.update(CLEAN_APP_HOUR_STATISTICS, time);
            logger.warn("clean_app_hour_statistics count={}", cleanCount);
            cleanCount = jdbcTemplate.update(CLEAN_APP_MINUTE_STATISTICS, time);
            logger.warn("clean_app_minute_statistics count={}", cleanCount);
            
            //清除客户端数据
            ClientReportCostDistriService clientReportCostDistriService = applicationContext.getBean(
                    "clientReportCostDistriService", ClientReportCostDistriService.class);
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -2);
            long timeFormat = NumberUtils.toLong(new SimpleDateFormat("yyyyMMddHHmmss").format(calendar.getTime()));
            cleanCount = clientReportCostDistriService.deleteBeforeCollectTime(timeFormat);
            logger.warn("clean_app_client_costtime_minute_stat count={}", cleanCount);
            
            
            ClientReportValueDistriService clientReportValueDistriService = applicationContext.getBean(
                    "clientReportValueDistriService", ClientReportValueDistriService.class);
            cleanCount = clientReportValueDistriService.deleteBeforeCollectTime(timeFormat);
            logger.warn("clean_app_client_value_distri_minute_stat count={}", cleanCount);
            
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -14);
            timeFormat = NumberUtils.toLong(new SimpleDateFormat("yyyyMMddHHmmss").format(calendar.getTime()));
            cleanCount = jdbcTemplate.update(CLEAN_APP_CLIENT_MINUTE_COST_TOTAL, timeFormat);
            logger.warn("clean_app_client_costtime_minute_stat_total count={}", cleanCount);
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }
    
}
