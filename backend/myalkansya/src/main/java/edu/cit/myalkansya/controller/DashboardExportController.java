package edu.cit.myalkansya.controller;

import edu.cit.myalkansya.service.DashboardExportService;
import edu.cit.myalkansya.security.JwtUtil;
import edu.cit.myalkansya.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DashboardExportController {

    @Autowired private DashboardExportService exportService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserService userService;

    @GetMapping("/dashboard")
    public void exportDashboardExcel(
            @RequestParam int month,
            @RequestParam int year,
            @RequestHeader("Authorization") String token,
            HttpServletResponse response
    ) {
        try {
            // Extract user info
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).orElseThrow().getUserId();

            // Prepare filename
            String filename = "MyAlkansya_DashboardExport_" + year + "_" + month + ".xlsx";
            
            // Set proper response headers with the correct MIME type for XLSX
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + 
                              URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()) + "\"");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            
            // Get the Excel data
            byte[] excelBytes = exportService.exportDashboardDataAsBytes(userId, month, year);
            
            // Set content length to ensure complete data transfer
            response.setContentLength(excelBytes.length);
            
            // Write directly to response without closing the stream
            response.getOutputStream().write(excelBytes);
            response.getOutputStream().flush();
            // Don't close the output stream - the container will do that
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.getWriter().write("Error exporting Excel: " + e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
