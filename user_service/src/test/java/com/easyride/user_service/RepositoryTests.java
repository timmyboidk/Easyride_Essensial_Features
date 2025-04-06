package com.easyride.user_service.repository;

import com.easyride.user_service.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

@DataJpaTest
public class RepositoryTests {

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Test
    void testPassengerRepository_CRUD() {
        Passenger p = new Passenger("testUser", "encryptedPass", "test@example.com");
        Passenger saved = passengerRepository.save(p);
        Optional<Passenger> found = passengerRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testUser");
    }

    @Test
    void testDriverRepository_CRUD() {
        Driver d = new Driver("driverUser", "encryptedPass", "driver@example.com");
        Driver saved = driverRepository.save(d);
        Optional<Driver> found = driverRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("driverUser");
    }

    @Test
    void testAdminRepository_CRUD() {
        Admin a = new Admin("adminUser", "encryptedPass", "admin@example.com");
        Admin saved = adminRepository.save(a);
        Optional<Admin> found = adminRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("adminUser");
    }
}
