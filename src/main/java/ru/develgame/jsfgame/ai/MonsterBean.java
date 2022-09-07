package ru.develgame.jsfgame.ai;

import ru.develgame.jsfgame.PersonsRegistry;
import ru.develgame.jsfgame.domain.Person;
import ru.develgame.jsfgame.domain.PersonType;
import ru.develgame.jsfgame.jms.ChangesListener;
import ru.develgame.jsfgame.jms.MessagesType;
import ru.develgame.jsfgame.location.PathFinder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ilya Zemskov
 */
@Named
@ApplicationScoped
public class MonsterBean implements MessageListener {
    @Inject
    private transient Logger logger;

    @EJB
    private PersonsRegistry personsRegistry;

    @Inject
    private ChangesListener changesListener;

    @Inject
    private PathFinder pathFinder;

    @Inject
    private Person monster;

    private List<Person> otherPersons;

    private boolean needUpdateOtherPersonsList = false;

    @PostConstruct
    public void init() {
        monster.setPersonType(PersonType.PERSON_TYPE4);
        monster.setImageLeft(800);
        monster.setImageTop(500);

        otherPersons = readOtherPersonsList();
        changesListener.addListener(this);
    }

    @PreDestroy
    public void finite() {
        changesListener.removeListener(this);
    }

    public void updatePerson() {
        if (!getOtherPersons().isEmpty()) {
            Person target = otherPersons.get(otherPersons.size() - 1);

            monster.setDirection(pathFinder.getNextStep(monster.getImageLeft(), monster.getImageTop(),
                    target.getImageLeft(), target.getImageTop()));
            monster.setMoving(true);
        }
        else
            monster.setMoving(false);

        monster.updateImage();
        monster.updateTopPosition();
        monster.updateLeftPosition();
    }

    private List<Person> readOtherPersonsList() {
        return personsRegistry.getOtherPersonsList(monster.getUuid());
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (((TextMessage) message).getText().equals(MessagesType.PERSON.toString())) {
                needUpdateOtherPersonsList = true;
            }
        } catch (JMSException e) {
            logger.log(Level.SEVERE, "Cannot get JMS message", e);
        }
    }

    public List<Person> getOtherPersons() {
        if (needUpdateOtherPersonsList) {
            otherPersons = readOtherPersonsList();
            needUpdateOtherPersonsList = false;
        }

        return otherPersons;
    }
}