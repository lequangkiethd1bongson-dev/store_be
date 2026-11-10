package vn.team4.store.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.team4.store.service.AdminManagementService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    public AdminManagementController(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    // -------------------------------------------------------------------------
    // CỬA HÀNG & DASHBOARD THÔNG TIN / DOANH THU CỬA HÀNG
    // -------------------------------------------------------------------------

    @GetMapping("/stores")
    public List<Map<String, Object>> getStores() {
        return adminManagementService.getStores();
    }

    @GetMapping("/admin/stores/revenue")
    public List<Map<String, Object>> getStoreRevenueSummaries() {
        return adminManagementService.getStoreRevenueSummaries();
    }

    // -------------------------------------------------------------------------
    // QUẢN LÝ TÀI KHOẢN NHÂN VIÊN (XEM, LỌC, SỬA THÔNG TIN, KÍCH HOẠT)
    // -------------------------------------------------------------------------

    @GetMapping("/admin/employees")
    public List<Map<String, Object>> getEmployees(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String warehouseId
    ) {
        return adminManagementService.getEmployees(role, isActive, search, storeId, warehouseId);
    }

    @GetMapping("/admin/employees/{id}")
    public Map<String, Object> getEmployeeById(@PathVariable UUID id) {
        return adminManagementService.getEmployeeById(id);
    }

    @PutMapping("/admin/employees/{id}")
    public Map<String, Object> updateEmployee(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body
    ) {
        return adminManagementService.updateEmployee(id, body);
    }

    @PatchMapping("/admin/employees/{id}/status")
    public Map<String, Object> updateEmployeeStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body
    ) {
        boolean isActive = body.get("isActive") == null
                || Boolean.parseBoolean(String.valueOf(body.get("isActive")));
        return adminManagementService.updateEmployeeStatus(id, isActive);
    }

    // -------------------------------------------------------------------------
    // CHẤM CÔNG NHÂN VIÊN THEO CƠ SỞ & ĐÁNH DẤU ĐANG LÀM / ĐANG NGHỈ
    // -------------------------------------------------------------------------

    @GetMapping("/admin/attendance")
    public List<Map<String, Object>> getAttendanceList(
            @RequestParam(required = false) String workDate,
            @RequestParam(required = false) String monthYear,
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String warehouseId
    ) {
        return adminManagementService.getAttendanceList(workDate, monthYear, storeId, warehouseId);
    }

    @PostMapping("/admin/attendance/status")
    public Map<String, Object> markAttendanceStatus(@RequestBody Map<String, Object> body) {
        return adminManagementService.markAttendanceStatus(body);
    }

    @PatchMapping("/admin/attendance/work-days")
    public Map<String, Object> updateAttendanceWorkDays(@RequestBody Map<String, Object> body) {
        return adminManagementService.updateAttendanceWorkDays(body);
    }

    // -------------------------------------------------------------------------
    // PHÂN CA & BẢNG CÔNG
    // -------------------------------------------------------------------------

    @GetMapping("/admin/shifts")
    public List<Map<String, Object>> getShifts(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String warehouseId
    ) {
        return adminManagementService.getShifts(startDate, endDate, storeId, warehouseId);
    }

    @PostMapping("/admin/shifts")
    public Map<String, Object> assignShift(@RequestBody Map<String, Object> body) {
        return adminManagementService.assignShift(body);
    }

    @GetMapping("/admin/timesheets")
    public List<Map<String, Object>> getTimesheets(
            @RequestParam(required = false) String monthYear,
            @RequestParam(required = false) String status
    ) {
        return adminManagementService.getTimesheets(monthYear, status);
    }

    // -------------------------------------------------------------------------
    // QUẢN LÝ THỐNG KÊ CƠ SỞ KHO & CỬA HÀNG (LƯỢNG HÀNG & LƯỢNG BÁN / XUẤT)
    // -------------------------------------------------------------------------

    @GetMapping("/admin/statistics/facilities")
    public List<Map<String, Object>> getFacilityStatistics() {
        return adminManagementService.getFacilityStatistics();
    }

    // -------------------------------------------------------------------------
    // HỒ SƠ CÁ NHÂN ADMIN / NHÂN VIÊN
    // -------------------------------------------------------------------------

    @GetMapping("/staff/profile")
    public Map<String, Object> getStaffProfile(Authentication authentication) {
        return adminManagementService.getMyProfile(authentication);
    }

    @PutMapping("/staff/profile")
    public Map<String, Object> updateStaffProfile(
            Authentication authentication,
            @RequestBody Map<String, Object> body
    ) {
        return adminManagementService.updateMyProfile(authentication, body);
    }

    @PostMapping("/staff/profile/change-password")
    public Map<String, Object> changeOwnPassword(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "OK");
        res.put("message", "Yêu cầu đổi mật khẩu đã được ghi nhận.");
        return res;
    }
}

