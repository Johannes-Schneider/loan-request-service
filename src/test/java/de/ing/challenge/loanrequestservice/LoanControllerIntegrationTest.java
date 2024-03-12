package de.ing.challenge.loanrequestservice;

import de.ing.challenge.loanrequestservice.dao.Customer;
import de.ing.challenge.loanrequestservice.dao.CustomerDao;
import de.ing.challenge.loanrequestservice.dao.LoanRequest;
import de.ing.challenge.loanrequestservice.dao.LoanRequestDao;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ResourceLock(TestResources.DATABASE)
@ResourceLock(TestResources.LOAN_SUM_CACHE)
class LoanControllerIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private CustomerDao customerDao;
    @Autowired
    private LoanRequestDao loanRequestDao;
    @Autowired
    private LoanSumCache loanSumCache;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        loanRequestDao.deleteAll();
        customerDao.deleteAll();
        loanSumCache.reset();
    }

    @Test
    @SneakyThrows
    void createLoanRequest_Success() {
        final long id = 42;
        final long customerId = 1337;
        final String payload = """
                {
                   "id": %d,
                   "amount": 1337.42,
                   "customerId": %d,
                   "customerFullName": "Customer Full Name"
                 }""".formatted(id, customerId);

        final MvcResult result = mvc.perform(post("/api/v1/loan-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEmpty();
        assertThat(customerDao.existsById(customerId)).isTrue();
        assertThat(loanRequestDao.existsById(id)).isTrue();
    }

    @Test
    @SneakyThrows
    void createLoanRequest_ConstrainViolation() {
        final long id = 42;
        final long customerId = 1337;
        final String payload = """
                {
                   "id": %d,
                   "amount": 1337.42,
                   "customerId": %d,
                   "customerFullName": ""
                 }""".formatted(id, customerId);

        final MvcResult result = mvc.perform(post("/api/v1/loan-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("customer full name must not be blank");
        assertThat(customerDao.existsById(customerId)).isFalse();
        assertThat(loanRequestDao.existsById(id)).isFalse();
    }

    @Test
    @SneakyThrows
    @Transactional
    void getLoanSumByCustomerId_Success() {
        final Customer customer = new Customer(42, "Customer");
        customerDao.saveAndFlush(customer);

        final LoanRequest loanRequest = new LoanRequest(1337, BigDecimal.valueOf(13.37d), customer);
        loanRequestDao.saveAndFlush(loanRequest);

        mvc.perform(get("/api/v1/loan-requests/sum/%s".formatted(customer.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.sum").value("13.37"));
    }

    @Test
    @SneakyThrows
    void getLoanSumByCustomerId_CustomerNotFound() {
        final long id = 42;

        final MvcResult result = mvc.perform(get("/api/v1/loan-requests/sum/%s".formatted(id)))
                .andExpect(status().isNotFound())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    @SneakyThrows
    void getLoanSumByCustomerId_InvalidCustomerId() {
        final String id = "invalid";

        final MvcResult result = mvc.perform(get("/api/v1/loan-requests/sum/%s".formatted(id)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }
}
