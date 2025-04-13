package primerriva.users_services.services;

import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.models.Users;

public interface UsersService {
    /**
     * This method is used to get the user by id.
     *
     * @param id the id of the user
     * @return the user
     */
    Users getUserByEmail(String email);

    /**
     * This method is used to create a new user.
     *
     * @param user the user to be created
     * @return the created user
     */
    Users createUser(UsersDto user);

    /**
     * This method is used to update an existing user.
     *
     * @param id   the id of the user to be updated
     * @param user the updated user
     * @return the updated user
     */
    Users updateUser(Long id, UsersDto user);

    /**
     * This method is used to delete a user by id.
     *
     * @param id the id of the user to be deleted
     */
    void deleteUser(Long id);
}
