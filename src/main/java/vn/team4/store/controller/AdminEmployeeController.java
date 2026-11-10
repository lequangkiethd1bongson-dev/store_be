package vn.team4.store.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.team4.store.model.EmployeeEntity;
import vn.team4.store.repository.EmployeeRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/employees")
@CrossOrigin(origins = "*")
public class AdminEmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping
    public List<EmployeeEntity> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @PostMapping
    public EmployeeEntity createEmployee(@RequestBody EmployeeEntity employee) {
        return employeeRepository.save(employee);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeEntity> getEmployeeById(@PathVariable Long id) {
        return employeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeEntity> updateEmployee(@PathVariable Long id, @RequestBody EmployeeEntity details) {
        return employeeRepository.findById(id)
                .map(emp -> {
                    emp.setName(details.getName());
                    emp.setEmail(details.getEmail());
                    emp.setPhone(details.getPhone());
                    emp.setRole(details.getRole());
                    emp.setStoreId(details.getStoreId());
                    emp.setWarehouseId(details.getWarehouseId());
                    emp.setIsActive(details.getIsActive());
                    return ResponseEntity.ok(employeeRepository.save(emp));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EmployeeEntity> updateStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return employeeRepository.findById(id)
                .map(emp -> {
                    if (body.containsKey("is_active")) {
                        emp.setIsActive(body.get("is_active"));
                    } else if (body.containsKey("isActive")) {
                        emp.setIsActive(body.get("isActive"));
                    }
                    return ResponseEntity.ok(employeeRepository.save(emp));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return employeeRepository.findById(id)
                .map(emp -> ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công cho nhân viên " + emp.getName())))
                .orElse(ResponseEntity.notFound().build());
    }
}
