package com.seminary.sms.repository;
import com.seminary.sms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUserId(String userId);
    boolean existsByUsername(String username);
}
