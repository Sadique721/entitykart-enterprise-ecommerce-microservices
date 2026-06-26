package com.entitykart.commonservices.notification.controller;

import com.entitykart.commonservices.notification.service.EmailService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Admin data-export controller (originally from notification-service).
 * Provides Excel (.xlsx) and Word (.doc) export for all entity types,
 * plus an email delivery endpoint that sends both formats as attachments.
 *
 * Base URL: /api/admin/export
 */
@RestController
@RequestMapping("/api/admin/export")
@Slf4j
public class AdminExportController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EmailService emailService;

    // ── Safe type-casting helpers ─────────────────────────────────────────────

    private String getStr(Map<String, Object> map, String key) {
        return map.get(key) != null ? map.get(key).toString() : "";
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }

    private long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        return 0L;
    }

    private int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchList(String serviceUrl) {
        try {
            return restTemplate.getForObject(serviceUrl, List.class);
        } catch (Exception e) {
            log.error("Failed to fetch list from {}: {}", serviceUrl, e.getMessage());
            return List.of();
        }
    }

    // ==================== EXCEL EXPORTS ====================

    @GetMapping("/orders/excel")
    public void exportOrdersToExcel(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://order-service/api/orders/all");
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Orders");
        String[] cols = {"Order ID", "Customer ID", "Address ID", "Total Amount", "Order Status",
                         "Payment Status", "Order Date", "Created At"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (Map<String, Object> o : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(getLong(o, "orderId"));
            row.createCell(1).setCellValue(getLong(o, "customerId"));
            row.createCell(2).setCellValue(getLong(o, "addressId"));
            row.createCell(3).setCellValue(getDouble(o, "totalAmount"));
            row.createCell(4).setCellValue(getStr(o, "orderStatus"));
            row.createCell(5).setCellValue(getStr(o, "paymentStatus"));
            row.createCell(6).setCellValue(getStr(o, "orderDate"));
            row.createCell(7).setCellValue(getStr(o, "createdAt"));
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "orders.xlsx");
    }

    @GetMapping("/products/excel")
    public void exportProductsToExcel(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://product-service/api/products/all");
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Products");
        String[] cols = {"Product ID", "Name", "Brand", "Description", "Price", "MRP", "Stock",
                         "SKU", "Category ID", "SubCategory ID", "Seller ID", "Created At", "Discount %"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (Map<String, Object> p : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(getLong(p, "productId"));
            row.createCell(1).setCellValue(getStr(p, "productName"));
            row.createCell(2).setCellValue(getStr(p, "brand"));
            row.createCell(3).setCellValue(getStr(p, "description"));
            row.createCell(4).setCellValue(getDouble(p, "price"));
            row.createCell(5).setCellValue(getDouble(p, "mrp"));
            row.createCell(6).setCellValue(getLong(p, "stockQuantity"));
            row.createCell(7).setCellValue(getStr(p, "sku"));
            row.createCell(8).setCellValue(getLong(p, "categoryId"));
            row.createCell(9).setCellValue(getLong(p, "subCategoryId"));
            row.createCell(10).setCellValue(getLong(p, "sellerId"));
            row.createCell(11).setCellValue(getStr(p, "createdAt"));
            row.createCell(12).setCellValue(getDouble(p, "discountPercent"));
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "products.xlsx");
    }

    @GetMapping("/users/excel")
    public void exportUsersToExcel(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://user-service/api/users/all");
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Users");
        String[] cols = {"User ID", "Name", "Email", "Role", "Active"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (Map<String, Object> u : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(getLong(u, "id"));
            row.createCell(1).setCellValue(getStr(u, "name"));
            row.createCell(2).setCellValue(getStr(u, "email"));
            row.createCell(3).setCellValue(getStr(u, "role"));
            row.createCell(4).setCellValue(getStr(u, "active"));
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "users.xlsx");
    }

    @GetMapping("/payments/excel")
    public void exportPaymentsToExcel(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://payment-service/api/payments/all");
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Payments");
        String[] cols = {"Payment ID", "Order ID", "Amount", "Mode", "Transaction Ref", "Status", "Payment Date"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (Map<String, Object> p : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(getLong(p, "paymentId"));
            row.createCell(1).setCellValue(getLong(p, "orderId"));
            row.createCell(2).setCellValue(getDouble(p, "amount"));
            row.createCell(3).setCellValue(getStr(p, "paymentMode"));
            row.createCell(4).setCellValue(getStr(p, "transactionRef"));
            row.createCell(5).setCellValue(getStr(p, "paymentStatus"));
            row.createCell(6).setCellValue(getStr(p, "paymentDate"));
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "payments.xlsx");
    }

    @GetMapping("/returns/excel")
    public void exportReturnsToExcel(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://return-service/api/admin/returns");
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Returns");
        String[] cols = {"Return ID", "Order ID", "Customer ID", "Product ID", "Quantity",
                         "Reason", "Status", "Refund Amount", "Admin Note", "Rejection Reason", "Created At"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (Map<String, Object> rt : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(getLong(rt, "returnId"));
            row.createCell(1).setCellValue(getLong(rt, "orderId"));
            row.createCell(2).setCellValue(getLong(rt, "customerId"));
            row.createCell(3).setCellValue(getLong(rt, "productId"));
            row.createCell(4).setCellValue(getLong(rt, "quantity"));
            row.createCell(5).setCellValue(getStr(rt, "reason"));
            row.createCell(6).setCellValue(getStr(rt, "status"));
            row.createCell(7).setCellValue(getDouble(rt, "refundAmount"));
            row.createCell(8).setCellValue(getStr(rt, "adminNote"));
            row.createCell(9).setCellValue(getStr(rt, "rejectionReason"));
            row.createCell(10).setCellValue(getStr(rt, "createdAt"));
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "returns.xlsx");
    }

    @GetMapping("/reviews/excel")
    public void exportReviewsToExcel(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://review-service/api/admin/reviews/all");
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Reviews");
        String[] cols = {"Review ID", "Product ID", "Customer ID", "Rating", "Comment", "Created At"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (Map<String, Object> rev : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(getLong(rev, "reviewId"));
            row.createCell(1).setCellValue(getLong(rev, "productId"));
            row.createCell(2).setCellValue(getLong(rev, "customerId"));
            row.createCell(3).setCellValue(getLong(rev, "rating"));
            row.createCell(4).setCellValue(getStr(rev, "comment"));
            row.createCell(5).setCellValue(getStr(rev, "createdAt"));
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "reviews.xlsx");
    }

    @GetMapping("/wishlist/excel")
    public void exportWishlistToExcel(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://wishlist-service/api/wishlist/all");
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Wishlist");
        String[] cols = {"Wishlist ID", "Product ID", "Product Name", "Price", "Added At"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (Map<String, Object> w : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(getLong(w, "wishlistId"));
            row.createCell(1).setCellValue(getLong(w, "productId"));
            row.createCell(2).setCellValue(getStr(w, "productName"));
            row.createCell(3).setCellValue(getDouble(w, "price"));
            row.createCell(4).setCellValue(getStr(w, "addedAt"));
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "wishlist.xlsx");
    }

    // ==================== WORD EXPORTS ====================

    @GetMapping("/orders/word")
    public void exportOrdersToWord(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://order-service/api/orders/all");
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=orders.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Order Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (Map<String, Object> o : list) {
                writer.printf("Order #%d | Customer #%d | Total ₹%.2f | Status %s | Payment %s | Date %s%n",
                        getLong(o, "orderId"), getLong(o, "customerId"), getDouble(o, "totalAmount"),
                        getStr(o, "orderStatus"), getStr(o, "paymentStatus"), getStr(o, "orderDate"));
            }
        }
    }

    @GetMapping("/products/word")
    public void exportProductsToWord(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://product-service/api/products/all");
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=products.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Product Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (Map<String, Object> p : list) {
                writer.printf("Product #%d | %s | Brand %s | Price ₹%.2f | Stock %d | SKU %s%n",
                        getLong(p, "productId"), getStr(p, "productName"), getStr(p, "brand"),
                        getDouble(p, "price"), getLong(p, "stockQuantity"), getStr(p, "sku"));
            }
        }
    }

    @GetMapping("/users/word")
    public void exportUsersToWord(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://user-service/api/users/all");
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=users.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("User Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (Map<String, Object> u : list) {
                writer.printf("User #%d | %s | %s | Role %s | Active %s%n",
                        getLong(u, "id"), getStr(u, "name"), getStr(u, "email"), getStr(u, "role"), getStr(u, "active"));
            }
        }
    }

    @GetMapping("/payments/word")
    public void exportPaymentsToWord(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://payment-service/api/payments/all");
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=payments.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Payment Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (Map<String, Object> p : list) {
                writer.printf("Payment #%d | Order #%d | ₹%.2f | Mode %s | Status %s | Ref %s%n",
                        getLong(p, "paymentId"), getLong(p, "orderId"), getDouble(p, "amount"),
                        getStr(p, "paymentMode"), getStr(p, "paymentStatus"), getStr(p, "transactionRef"));
            }
        }
    }

    @GetMapping("/returns/word")
    public void exportReturnsToWord(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://return-service/api/admin/returns");
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=returns.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Return Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (Map<String, Object> rt : list) {
                writer.printf("Return #%d | Order #%d | Status %s | Reason %s | Refund ₹%.2f%n",
                        getLong(rt, "returnId"), getLong(rt, "orderId"), getStr(rt, "status"),
                        getStr(rt, "reason"), getDouble(rt, "refundAmount"));
            }
        }
    }

    @GetMapping("/reviews/word")
    public void exportReviewsToWord(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://review-service/api/admin/reviews/all");
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=reviews.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Review Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (Map<String, Object> r : list) {
                writer.printf("Review #%d | Product #%d | Customer #%d | Rating %d/5 | Comment: %s%n",
                        getLong(r, "reviewId"), getLong(r, "productId"), getLong(r, "customerId"),
                        getInt(r, "rating"), getStr(r, "comment"));
            }
        }
    }

    @GetMapping("/wishlist/word")
    public void exportWishlistToWord(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = fetchList("http://wishlist-service/api/wishlist/all");
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=wishlist.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Wishlist Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (Map<String, Object> w : list) {
                writer.printf("Wishlist #%d | Product #%d | Name %s | Price ₹%.2f | Added %s%n",
                        getLong(w, "wishlistId"), getLong(w, "productId"), getStr(w, "productName"),
                        getDouble(w, "price"), getStr(w, "addedAt"));
            }
        }
    }

    // ==================== EMAIL REPORT (Excel + Word attachment) ====================

    @PostMapping("/send-report")
    public String sendReportEmail(@RequestParam String reportType, @RequestParam String email) {
        try {
            byte[] excelData = generateExcelReportBytes(reportType);
            byte[] wordData  = generateWordReportBytes(reportType);
            emailService.sendReportWithAttachments(email, reportType, excelData, wordData);
            return "Report sent successfully to " + email;
        } catch (Exception e) {
            log.error("Failed to send report for type: {}", reportType, e);
            throw new RuntimeException("Failed to send report: " + e.getMessage());
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private void createHeaderRow(Sheet sheet, String[] cols) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            header.createCell(i).setCellValue(cols[i]);
        }
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            try { sheet.autoSizeColumn(i); } catch (Exception ignored) { }
        }
    }

    private void writeResponse(HttpServletResponse response, Workbook wb, String filename) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        wb.write(response.getOutputStream());
        wb.close();
    }

    private byte[] generateExcelReportBytes(String reportType) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(reportType);
            String[] cols = null;
            int r = 1;
            if ("orders".equalsIgnoreCase(reportType)) {
                cols = new String[]{"Order ID", "Customer ID", "Address ID", "Total Amount", "Order Status", "Payment Status", "Order Date", "Created At"};
                createHeaderRow(sheet, cols);
                for (Map<String, Object> o : fetchList("http://order-service/api/orders/all")) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(getLong(o, "orderId")); row.createCell(1).setCellValue(getLong(o, "customerId"));
                    row.createCell(2).setCellValue(getLong(o, "addressId")); row.createCell(3).setCellValue(getDouble(o, "totalAmount"));
                    row.createCell(4).setCellValue(getStr(o, "orderStatus")); row.createCell(5).setCellValue(getStr(o, "paymentStatus"));
                    row.createCell(6).setCellValue(getStr(o, "orderDate")); row.createCell(7).setCellValue(getStr(o, "createdAt"));
                }
            } else if ("products".equalsIgnoreCase(reportType)) {
                cols = new String[]{"Product ID", "Name", "Brand", "Price", "Stock", "Discount %"};
                createHeaderRow(sheet, cols);
                for (Map<String, Object> p : fetchList("http://product-service/api/products/all")) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(getLong(p, "productId")); row.createCell(1).setCellValue(getStr(p, "productName"));
                    row.createCell(2).setCellValue(getStr(p, "brand")); row.createCell(3).setCellValue(getDouble(p, "price"));
                    row.createCell(4).setCellValue(getLong(p, "stockQuantity")); row.createCell(5).setCellValue(getDouble(p, "discountPercent"));
                }
            } else if ("users".equalsIgnoreCase(reportType)) {
                cols = new String[]{"User ID", "Name", "Email", "Role", "Active"};
                createHeaderRow(sheet, cols);
                for (Map<String, Object> u : fetchList("http://user-service/api/users/all")) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(getLong(u, "id")); row.createCell(1).setCellValue(getStr(u, "name"));
                    row.createCell(2).setCellValue(getStr(u, "email")); row.createCell(3).setCellValue(getStr(u, "role"));
                    row.createCell(4).setCellValue(getStr(u, "active"));
                }
            } else if ("payments".equalsIgnoreCase(reportType)) {
                cols = new String[]{"Payment ID", "Order ID", "Amount", "Mode", "Transaction Ref", "Status", "Payment Date"};
                createHeaderRow(sheet, cols);
                for (Map<String, Object> p : fetchList("http://payment-service/api/payments/all")) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(getLong(p, "paymentId")); row.createCell(1).setCellValue(getLong(p, "orderId"));
                    row.createCell(2).setCellValue(getDouble(p, "amount")); row.createCell(3).setCellValue(getStr(p, "paymentMode"));
                    row.createCell(4).setCellValue(getStr(p, "transactionRef")); row.createCell(5).setCellValue(getStr(p, "paymentStatus"));
                    row.createCell(6).setCellValue(getStr(p, "paymentDate"));
                }
            } else if ("returns".equalsIgnoreCase(reportType)) {
                cols = new String[]{"Return ID", "Order ID", "Customer ID", "Product ID", "Quantity", "Reason", "Status", "Refund Amount", "Created At"};
                createHeaderRow(sheet, cols);
                for (Map<String, Object> rt : fetchList("http://return-service/api/admin/returns")) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(getLong(rt, "returnId")); row.createCell(1).setCellValue(getLong(rt, "orderId"));
                    row.createCell(2).setCellValue(getLong(rt, "customerId")); row.createCell(3).setCellValue(getLong(rt, "productId"));
                    row.createCell(4).setCellValue(getLong(rt, "quantity")); row.createCell(5).setCellValue(getStr(rt, "reason"));
                    row.createCell(6).setCellValue(getStr(rt, "status")); row.createCell(7).setCellValue(getDouble(rt, "refundAmount"));
                    row.createCell(8).setCellValue(getStr(rt, "createdAt"));
                }
            } else if ("reviews".equalsIgnoreCase(reportType)) {
                cols = new String[]{"Review ID", "Product ID", "Customer ID", "Rating", "Comment", "Created At"};
                createHeaderRow(sheet, cols);
                for (Map<String, Object> rev : fetchList("http://review-service/api/admin/reviews/all")) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(getLong(rev, "reviewId")); row.createCell(1).setCellValue(getLong(rev, "productId"));
                    row.createCell(2).setCellValue(getLong(rev, "customerId")); row.createCell(3).setCellValue(getLong(rev, "rating"));
                    row.createCell(4).setCellValue(getStr(rev, "comment")); row.createCell(5).setCellValue(getStr(rev, "createdAt"));
                }
            } else if ("wishlist".equalsIgnoreCase(reportType)) {
                cols = new String[]{"Wishlist ID", "Product ID", "Product Name", "Price", "Added At"};
                createHeaderRow(sheet, cols);
                for (Map<String, Object> w : fetchList("http://wishlist-service/api/wishlist/all")) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(getLong(w, "wishlistId")); row.createCell(1).setCellValue(getLong(w, "productId"));
                    row.createCell(2).setCellValue(getStr(w, "productName")); row.createCell(3).setCellValue(getDouble(w, "price"));
                    row.createCell(4).setCellValue(getStr(w, "addedAt"));
                }
            }
            if (cols != null) autoSizeColumns(sheet, cols.length);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    private byte[] generateWordReportBytes(String reportType) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(baos)) {
            writer.println(reportType.toUpperCase() + " REPORT");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            if ("orders".equalsIgnoreCase(reportType)) {
                for (Map<String, Object> o : fetchList("http://order-service/api/orders/all"))
                    writer.printf("Order #%d | Customer #%d | Total ₹%.2f | Status %s | Payment %s | Date %s%n",
                            getLong(o, "orderId"), getLong(o, "customerId"), getDouble(o, "totalAmount"),
                            getStr(o, "orderStatus"), getStr(o, "paymentStatus"), getStr(o, "orderDate"));
            } else if ("products".equalsIgnoreCase(reportType)) {
                for (Map<String, Object> p : fetchList("http://product-service/api/products/all"))
                    writer.printf("Product #%d | %s | Brand %s | Price ₹%.2f | Stock %d | SKU %s%n",
                            getLong(p, "productId"), getStr(p, "productName"), getStr(p, "brand"),
                            getDouble(p, "price"), getLong(p, "stockQuantity"), getStr(p, "sku"));
            } else if ("users".equalsIgnoreCase(reportType)) {
                for (Map<String, Object> u : fetchList("http://user-service/api/users/all"))
                    writer.printf("User #%d | %s | %s | Role %s | Active %s%n",
                            getLong(u, "id"), getStr(u, "name"), getStr(u, "email"), getStr(u, "role"), getStr(u, "active"));
            } else if ("payments".equalsIgnoreCase(reportType)) {
                for (Map<String, Object> p : fetchList("http://payment-service/api/payments/all"))
                    writer.printf("Payment #%d | Order #%d | ₹%.2f | Mode %s | Status %s | Ref %s%n",
                            getLong(p, "paymentId"), getLong(p, "orderId"), getDouble(p, "amount"),
                            getStr(p, "paymentMode"), getStr(p, "paymentStatus"), getStr(p, "transactionRef"));
            } else if ("returns".equalsIgnoreCase(reportType)) {
                for (Map<String, Object> rt : fetchList("http://return-service/api/admin/returns"))
                    writer.printf("Return #%d | Order #%d | Status %s | Reason %s | Refund ₹%.2f%n",
                            getLong(rt, "returnId"), getLong(rt, "orderId"), getStr(rt, "status"),
                            getStr(rt, "reason"), getDouble(rt, "refundAmount"));
            } else if ("reviews".equalsIgnoreCase(reportType)) {
                for (Map<String, Object> r : fetchList("http://review-service/api/admin/reviews/all"))
                    writer.printf("Review #%d | Product #%d | Customer #%d | Rating %d/5 | Comment: %s%n",
                            getLong(r, "reviewId"), getLong(r, "productId"), getLong(r, "customerId"),
                            getInt(r, "rating"), getStr(r, "comment"));
            } else if ("wishlist".equalsIgnoreCase(reportType)) {
                for (Map<String, Object> w : fetchList("http://wishlist-service/api/wishlist/all"))
                    writer.printf("Wishlist #%d | Product #%d | Name %s | Price ₹%.2f | Added %s%n",
                            getLong(w, "wishlistId"), getLong(w, "productId"), getStr(w, "productName"),
                            getDouble(w, "price"), getStr(w, "addedAt"));
            }
        }
        return baos.toByteArray();
    }
}
