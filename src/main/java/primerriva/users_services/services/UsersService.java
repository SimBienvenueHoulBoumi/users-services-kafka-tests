package primerriva.users_services.services;

import primerriva.users_services.dto.UsersDto;

public interface UsersService {
    /**
     * This method is used to get the user by id.
     *
     * @param id the id of the user
     * @return the user
     */
    void getUserByEmail(String email);

    /**
     * This method is used to create a new user.
     *
     * @param user the user to be created
     * @return the created user
     */
    void createUser(UsersDto user);

    /**
     * This method is used to update an existing user.
     *
     * @param id   the id of the user to be updated
     * @param user the updated user
     * @return the updated user
     */
    void updateUser(Long id, UsersDto user);

    /**
     * This method is used to delete a user by id.
     *
     * @param id the id of the user to be deleted
     */
    void deleteUser(Long id);
}
