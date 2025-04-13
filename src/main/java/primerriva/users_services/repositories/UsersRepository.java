package primerriva.users_services.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import primerriva.users_services.models.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long>{
    Users findByEmail(String email);
}
