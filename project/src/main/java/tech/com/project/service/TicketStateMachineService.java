package tech.com.project.service;

import tech.com.project.model.Ticket;
import tech.com.project.model.TicketStatus;
import tech.com.project.model.UserAccount;

import java.util.List;

public interface TicketStateMachineService {

    /**
     * Valida de manera estricta que la transición del estado actual al estado destino
     * cumpla con las reglas del flujo de vida del ticket (RN-02) y que el actor cuente
     * con el rol y la autorización requerida.
     *
     * @param ticket       Ticket sobre el cual se ejecutará la transición.
     * @param targetStatus Estado destino al que se desea mover el ticket.
     * @param actor        Usuario autenticado que ejecuta la acción.
     * @param actorRoles   Lista de roles del usuario actor.
     */
    void validateTransition(Ticket ticket, TicketStatus targetStatus, UserAccount actor, List<String> actorRoles);
}
