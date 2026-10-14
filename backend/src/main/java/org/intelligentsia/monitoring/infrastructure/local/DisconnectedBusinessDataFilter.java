package org.intelligentsia.monitoring.infrastructure.local;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import java.io.IOException;
import java.util.*;

/** Honest empty data contracts while the external business source is disconnected. */
@Component
@Profile("local-config")
public class DisconnectedBusinessDataFilter extends OncePerRequestFilter {
    private final ObjectMapper json;
    public DisconnectedBusinessDataFilter(ObjectMapper json) { this.json=json; }

    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String path=request.getRequestURI();
        String permission=permission(path);
        if (permission==null) { chain.doFilter(request,response); return; }
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) { write(response,401,Map.of("code","UNAUTHORIZED","message","Connexion requise.")); return; }
        if (!permission.isEmpty() && authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals(permission))) {
            write(response,403,Map.of("code","ACCESS_DENIED","message","Accès refusé.")); return;
        }
        if (!request.getMethod().equals("GET") || path.contains("export") || path.contains("download") || path.endsWith("/report")) {
            write(response,503,Map.of("code","BUSINESS_DATABASE_DISCONNECTED","message","La base métier est déconnectée. Les opérations sur ses données sont suspendues.")); return;
        }
        Object data;
        if (path.equals("/api/dashboard/summary")) {
            Map<String,Object> summary=new LinkedHashMap<>();
            for (String key: List.of("totalAtms","atmsOnline","atmsOffline","atmsWarning","atmsUnknown","totalPos","posWithoutTelecollect","gabAlertsToday","totalFrauds")) summary.put(key,0);
            summary.put("networks",List.of()); summary.put("hosts",List.of()); data=summary;
        } else if (path.equals("/api/dashboard/history")) data=Map.of("atmAvailabilityHistory",List.of(),"atmAlertImpact",List.of(),"fraudHistory",List.of());
        else if (path.equals("/api/notifications/unread-count")) data=Map.of("count",0);
        else if (path.equals("/api/fraud/alerts")) {
            Map<String,Object> overview=new LinkedHashMap<>(page(request));
            overview.remove("content");
            for (String key: List.of("criticalCount","mediumCount","typicalCount","lessCriticalCount","lowCount","toQualifyCount","confirmedCount")) overview.put(key,0);
            for (String key: List.of("hourlyAlerts","topBins","topRules","alerts")) overview.put(key,List.of());
            data=overview;
        } else if (isPage(path)) data=page(request);
        else if (path.endsWith("/all") || path.endsWith("/latest") || path.endsWith("/history") || path.endsWith("/types") || path.endsWith("/networks") || path.contains("by-institution") || path.endsWith("/cassettes") || path.endsWith("/open-alert-ids")) data=List.of();
        else { write(response,404,Map.of("code","NO_BUSINESS_DATA","message","Aucune donnée disponible : la base métier est déconnectée.")); return; }
        write(response,200,ApiResponse.ok(data));
    }

    private String permission(String path) {
        if (path.startsWith("/api/dashboard/")) return "DASHBOARD_READ";
        if (path.startsWith("/api/atms") || path.startsWith("/api/gabs/")) return "ATM_READ";
        if (path.startsWith("/api/institutions")) return "INSTITUTION_READ";
        if (path.startsWith("/api/monitoring/alerts")) return "MONITORING_ALERT_VIEW";
        if (path.startsWith("/api/fraud/reports")) return "FRAUD_REPORT_VIEW";
        if (path.startsWith("/api/fraud/")) return "FRAUD_READ";
        if (path.startsWith("/api/transactions/")) return "TRANSACTION_READ";
        if (path.startsWith("/api/network/")) return "NETWORK_READ";
        if (path.startsWith("/api/host/")) return "HOST_READ";
        if (path.startsWith("/api/pos") || path.startsWith("/api/technicians")) return "POS_READ";
        if (path.startsWith("/api/fleet/")) return "FLEET_READ";
        if (path.startsWith("/api/extraction/jobs") || path.matches("/api/extraction/(atm|pos).*")) return "EXTRACTION_READ";
        if (path.startsWith("/api/notifications")) return "";
        return null;
    }
    private boolean isPage(String path) {
        return Set.of("/api/atms","/api/institutions","/api/monitoring/alerts","/api/fraud/rates","/api/fraud/details","/api/fraud/source","/api/fraud/reports/daily","/api/pos","/api/technicians","/api/fleet/accounts","/api/fleet/cards-over-limit","/api/transactions/rates","/api/transactions/errors","/api/transactions/operations","/api/notifications","/api/extraction/jobs").contains(path);
    }
    private Map<String,Object> page(HttpServletRequest request) {
        int number=parse(request.getParameter("page"),0),size=parse(request.getParameter("size"),20);
        return Map.of("content",List.of(),"page",Math.max(0,number),"size",Math.max(1,Math.min(100,size)),"totalElements",0,"totalPages",0,"first",true,"last",true);
    }
    private int parse(String value,int fallback) { try { return Integer.parseInt(value); } catch(Exception e) { return fallback; } }
    private void write(HttpServletResponse response,int status,Object body) throws IOException {
        response.setStatus(status); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8"); json.writeValue(response.getOutputStream(),body);
    }
}
