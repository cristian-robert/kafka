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
                        .setCSS(
                // Base styles for dark theme
                "body.dark { background-color: #1a1a1a !important; color: #ffffff !important; }" +
                ".dark .card { background-color: #2d2d2d !important; border: none !important; box-shadow: 0 2px 6px rgba(0,0,0,0.5) !important; }" +
                ".dark .card-header { background-color: #383838 !important; border-bottom: 1px solid #404040 !important; }" +
                
                // Status badges
                ".badge-pass { background-color: #00af00 !important; color: #ffffff !important; }" +
                ".badge-fail { background-color: #f44336 !important; color: #ffffff !important; }" +
                ".badge-skip { background-color: #ff9800 !important; color: #ffffff !important; }" +
                ".badge-warning { background-color: #ff9800 !important; color: #ffffff !important; }" +
                ".badge-info { background-color: #2196f3 !important; color: #ffffff !important; }" +
                
                // Dashboard improvements
                ".dark .dashboard-view { background-color: #1a1a1a !important; padding: 20px !important; }" +
                ".dark .dashboard-view .card:hover { transform: translateY(-2px); transition: all .3s ease; }" +
                
                // Tables
                ".dark .table { color: #ffffff !important; }" +
                ".dark .table td, .dark .table th { border-top: 1px solid #404040 !important; }" +
                ".dark .table thead th { border-bottom: 2px solid #404040 !important; }" +
                
                // Charts
                ".dark .charts-row { margin-top: 30px !important; }" +
                ".dark .dashboard-summary-chart { height: 350px !important; background-color: #2d2d2d !important; }" +
                
                // Navigation
                ".dark .nav-item { background-color: #2d2d2d !important; }" +
                ".dark .nav-item.active { background-color: #383838 !important; }" +
                ".dark .nav-link { color: #ffffff !important; }" +
                
                // Search and filters
                ".dark .form-control { background-color: #2d2d2d !important; border: 1px solid #404040 !important; color: #ffffff !important; }" +
                ".dark .form-control:focus { border-color: #2196f3 !important; box-shadow: 0 0 0 0.2rem rgba(33,150,243,.25) !important; }" +
                
                // Timeline
                ".dark .timeline-item-wrap { background-color: #2d2d2d !important; }" +
                ".dark .timeline-item { border-color: #404040 !important; }"
            )
            .setJS(
                // Remove default logo and add some animations
                "document.getElementsByClassName('logo')[0].style.display='none';" +
                
                // Enhance cards with hover effect
                "$('.card').hover(" +
                "function() { $(this).css('transform', 'translateY(-5px)'); }," +
                "function() { $(this).css('transform', 'translateY(0)'); }" +
                ");" +
                
                // Add smooth transitions
                "document.head.insertAdjacentHTML('beforeend', '<style>" +
                "* { transition: all .2s ease-in-out !important; }" +
                ".card { transition: transform .3s ease-in-out !important; }" +
                "</style>');"
            );
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
