package com.finedge.controller;

import com.finedge.dto.DisbursementCreateRequest;
import com.finedge.dto.DisbursementDTO;
import com.finedge.dto.OutstandingDTO;
import com.finedge.dto.PageResponse;
import com.finedge.dto.RepaymentCreateRequest;
import com.finedge.dto.RepaymentDTO;
import com.finedge.service.LoanDisbursementService;
import com.finedge.service.LoanRepaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disbursement and repayment operations for loan applications (Roadmap G).
 *
 * <p>Paths are sub-resources of {@code /api/etfdb/loan-applications},
 * consistent with existing financier review sub-resources
 * ({@code /financier/{id}/approve}, {@code /financier/{id}/reject}).
 *
 * <p>Authorisation:
 * <ul>
 *   <li>Recording a disbursement requires {@code ROLE_FINANCIER_ADMIN} and
 *       is scoped to the authenticated financier's own institution.</li>
 *   <li>Recording a repayment is borrower-scoped to the authenticated
 *       user's own applications.</li>
 *   <li>Reading disbursement, repayment history, and outstanding balance is
 *       scoped to the current user: financier-admin sees their own
 *       institution's applications; any other authenticated user sees only
 *       their own applications.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/etfdb/loan-applications")
public class LoanDisbursementController {

    private final LoanDisbursementService disbursementService;
    private final LoanRepaymentService repaymentService;

    public LoanDisbursementController(LoanDisbursementService disbursementService,
                                      LoanRepaymentService repaymentService) {
        this.disbursementService = disbursementService;
        this.repaymentService = repaymentService;
    }

    /**
     * Records a disbursement for an approved loan application owned by the
     * current financier's institution. Requires {@code ROLE_FINANCIER_ADMIN}.
     */
    @PostMapping("/{id}/disburse")
    @PreAuthorize("hasAuthority('ROLE_FINANCIER_ADMIN')")
    public ResponseEntity<DisbursementDTO> disburse(@PathVariable Integer id,
                                                    @Valid @RequestBody DisbursementCreateRequest request) {
        DisbursementDTO created = disbursementService.recordDisbursement(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Returns the persisted disbursement for a loan application, scoped to
     * the current user's ownership (financier-admin own institution, or
     * borrower own application).
     */
    @GetMapping("/{id}/disbursement")
    public ResponseEntity<DisbursementDTO> getDisbursement(@PathVariable Integer id) {
        return ResponseEntity.ok(disbursementService.getDisbursement(id));
    }

    /**
     * Records a repayment against a disbursed loan application owned by the
     * authenticated borrower.
     */
    @PostMapping("/{id}/repayments")
    public ResponseEntity<RepaymentDTO> repay(@PathVariable Integer id,
                                              @Valid @RequestBody RepaymentCreateRequest request) {
        RepaymentDTO created = repaymentService.recordRepayment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Returns the repayment history for a loan application, scoped to the
     * current user's ownership.
     */
    @GetMapping("/{id}/repayments")
    public ResponseEntity<PageResponse<RepaymentDTO>> listRepayments(@PathVariable Integer id,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(repaymentService.listRepayments(id, page, size));
    }

    /**
     * Returns the computed outstanding balance for a loan application,
     * scoped to the current user's ownership.
     */
    @GetMapping("/{id}/outstanding")
    public ResponseEntity<OutstandingDTO> outstanding(@PathVariable Integer id) {
        return ResponseEntity.ok(disbursementService.getOutstanding(id));
    }
}
