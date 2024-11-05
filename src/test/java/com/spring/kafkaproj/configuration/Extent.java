import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.aventstack.extentreports.reporter.configuration.ViewName;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "extent.reporter")
public class ExtentReportConfig {
    private SparkProperties spark;

    @Bean
    public ExtentReports extentReports() {
        ExtentReports extentReports = new ExtentReports();
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(spark.getOut());
        
        // Enhanced configuration for better dashboard
        sparkReporter.config()
            .setTheme(Theme.valueOf(spark.getTheme().toUpperCase()))
            .setDocumentTitle(spark.getDocumentTitle())
            .setReportName(spark.getReportName())
            .setTimeStampFormat(spark.getTimestampFormat())
            .enableTimeline(true)
            .setCSS(".badge-primary { background-color: #1967d2 !important; }" +
                   ".badge-success { background-color: #0a8040 !important; }" +
                   ".badge-danger { background-color: #dc3545 !important; }" +
                   ".dashboard-view { padding: 20px !important; }" +
                   ".dashboard-view .card { box-shadow: 0 2px 4px rgba(0,0,0,.1) !important; }" +
                   ".test-details-dropdown { padding: 10px !important; }" +
                   ".charts-row { margin-top: 20px !important; }" +
                   ".dashboard-summary-chart { height: 350px !important; }" +
                   ".environment-details { margin-top: 20px !important; }")
            .setJS("document.getElementsByClassName('logo')[0].style.display='none';" +
                  "document.getElementsByClassName('dashboard-view')[0].style.backgroundColor='#f8f9fa';" +
                  "$('.dashboard-view .card').hover(function(){$(this).addClass('shadow-lg')},function(){$(this).removeClass('shadow-lg')});");

        // Set view order for better navigation
        sparkReporter.viewConfigurer()
            .viewOrder()
            .as(new ViewName[] {
                ViewName.DASHBOARD,
                ViewName.TEST,
                ViewName.CATEGORY,
                ViewName.EXCEPTION
            })
            .apply();
            
        extentReports.attachReporter(sparkReporter);
        return extentReports;
    }

    @lombok.Data
    public static class SparkProperties {
        private String out;
        private String theme;
        private String documentTitle;
        private String reportName;
        private String timestampFormat;
    }

    // Getter and Setter
    public SparkProperties getSpark() {
        return spark;
    }

    public void setSpark(SparkProperties spark) {
        this.spark = spark;
    }
}
