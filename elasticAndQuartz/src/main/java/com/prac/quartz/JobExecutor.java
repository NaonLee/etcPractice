package com.prac.quartz;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;


public class JobExecutor implements Job {
    
    private static final SimpleDateFormat TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSSS"); 
    
    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        JobDataMap jobDataMap = ctx.getJobDetail().getJobDataMap();
        
        String current = TIMESTAMP.format(new Date());
        String triggerKey = ctx.getTrigger().getKey().toString();
        String message = jobDataMap.getString("message");
        
        System.out.println(String.format("[%s][%s] %s", current, triggerKey, message ));
    }
}
 
/**
 * Quartz Scheduler
 */
class JobLuacher {
    public static void main(String[] args) {
        try {
            
            // create Scheduler 
            SchedulerFactory sfactory = new StdSchedulerFactory();
            Scheduler scheduler = sfactory.getScheduler();
            
            scheduler.getListenerManager().addJobListener(new MyScheduleListener());
            scheduler.getListenerManager().addTriggerListener(new MyTriggerListener());
            
            // Scheduler 실행
            scheduler.start();
            
            // JOB Data 객체 생성
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("message", "This is Quartz");
            
            // JOB Executor Class
            Class<? extends Job> jobClass = JobExecutor.class;
 
            // JOB 생성
            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                                    .withIdentity("job_name", "job_group")
                                    .setJobData(jobDataMap)
                                    .build();
            
            // SimpleTrigger 생성
            // 4초마다 반복하며, 최대 5회 실행
            SimpleScheduleBuilder simpleSchedule = SimpleScheduleBuilder.repeatSecondlyForTotalCount(5, 4);
            SimpleTrigger simpleTrigger = (SimpleTrigger) TriggerBuilder.newTrigger()
                                            .withIdentity("simple_trigger", "simple_trigger_group")
                                            .withSchedule(simpleSchedule)
                                            .forJob(jobDetail)
                                            .build();
 
            // CronTrigger 생성
            // 15초주기로 반복( 0, 15, 30, 45 )
            CronScheduleBuilder cronSchedule = CronScheduleBuilder.cronSchedule(new CronExpression("0/15 * * * * ?"));
            CronTrigger cronTrigger = (CronTrigger) TriggerBuilder.newTrigger()
                                        .withIdentity("cron_trigger", "cron_trigger_group")
                                        .withSchedule(cronSchedule)
                                        .forJob(jobDetail)
                                        .build();
            
            // JobDtail : Trigger = 1 : N 설정
            Set<Trigger> triggerSet = new HashSet<Trigger>();
            triggerSet.add(simpleTrigger);
            triggerSet.add(cronTrigger);
 
            // Schedule 등록
            scheduler.scheduleJob(jobDetail, triggerSet, false);
            
        } catch (ParseException | SchedulerException e) {
            e.printStackTrace();
        }
    }
}
