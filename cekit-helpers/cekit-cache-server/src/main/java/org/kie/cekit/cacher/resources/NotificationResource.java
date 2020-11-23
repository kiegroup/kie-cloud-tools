package org.kie.cekit.cacher.resources;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.kie.cekit.cacher.notification.Notification;
import org.kie.cekit.cacher.objects.GChatMessage;
import org.kie.cekit.cacher.properties.CacherProperties;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.logging.Logger;

@Path("/notification")
public class NotificationResource {

    private Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private static final Validator VALIDATOR =
            Validation.byDefaultProvider()
                    .configure()
                    .messageInterpolator(new ParameterMessageInterpolator())
                    .buildValidatorFactory()
                    .getValidator();

    @Inject
    CacherProperties property;

    @Inject
    Notification notification;

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/send")
    public Response notify(GChatMessage message) {

        if (null == property.gChatWebhook()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Please set the org.kie.cekit.cacher.gchat.webhook property.").build();
        }

        Set<ConstraintViolation<GChatMessage>> violations = VALIDATOR.validate(message);
        if (violations.size() > 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(violations).build();
        }

        log.fine(String.format("Sending notification to configured webhook, message From: [%s], Subject: [%s], Receipts: [%s], Text: [%s].",
                message.getFrom(),
                message.getSubject(),
                message.getRecipients(),
                message.getMessage()));

        String[] recipients = new String[message.getRecipients().size()];
        recipients = message.getRecipients().toArray(recipients);

        StringBuilder response = new StringBuilder();
        response.append(notification.mention(recipients) + " f.y.i.:\n");
        response.append("New message from " + notification.bold(message.getFrom()) + ".\n");
        response.append("Subject " + notification.bold(message.getSubject()) + ".\n");
        response.append("Message content:\n " + notification.monospaceBlock(message.getMessage()));

        try {
            notification.send(response.toString(), property.gChatWebhook());
            return Response.ok("Message sent").build();
        } catch (final Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }
}
