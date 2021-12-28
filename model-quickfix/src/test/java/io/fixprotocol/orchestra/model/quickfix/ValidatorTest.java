/*
 * Copyright 2017-2020 FIX Protocol Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package io.fixprotocol.orchestra.model.quickfix;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.fixprotocol._2020.orchestra.repository.MessageType;
import io.fixprotocol._2020.orchestra.repository.Repository;
import io.fixprotocol.orchestra.message.TestException;
import io.fixprotocol.orchestra.model.SymbolResolver;

import quickfix.SessionID;
import quickfix.field.TradSesStatus;
import quickfix.field.TradingSessionID;
import quickfix.fix50sp2.TradingSessionStatus;

public class ValidatorTest {

  private QuickfixValidator validator;
  private static Repository repository;
  private RepositoryAccessor repositoryAdapter;
  private SessionID sessionID;

  @BeforeAll
  public static void setupOnce() throws Exception {
    repository = unmarshal(Thread.currentThread().getContextClassLoader().getResourceAsStream("mit_2016.xml"));
  }

  @BeforeEach
  public void setUp() throws Exception {
    sessionID = new SessionID("FIXT.1.1", "sender", "target");
    repositoryAdapter = new RepositoryAccessor(repository);
    final SymbolResolver symbolResolver = new SymbolResolver();
    //symbolResolver.setTrace(true);
    validator = new QuickfixValidator(repositoryAdapter, symbolResolver);
  }

  private static Repository unmarshal(InputStream inputFile) throws JAXBException {
    JAXBContext jaxbContext = JAXBContext.newInstance(Repository.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    return (Repository) jaxbUnmarshaller.unmarshal(inputFile);
  }

  @Test
  public void emptyMessage() {
    TradingSessionStatus message = new TradingSessionStatus();
    MessageType messageType = repositoryAdapter.getMessage("TradingSessionStatus", "base");
    try {
      validator.validate(message, messageType);
      fail("TestException expected");
    } catch (TestException e) {
      assertTrue(e.hasDetails());
      System.out.println(e.getMessage());
    }
  }

  @Test
  public void badCode() {
    TradingSessionStatus message = new TradingSessionStatus();
    message.set(new TradingSessionID(TradingSessionID.DAY));
    message.set(new TradSesStatus(82));
    MessageType messageType = repositoryAdapter.getMessage("TradingSessionStatus", "base");
    try {
      validator.validate(message, messageType);
      fail("TestException expected");
    } catch (TestException e) {
      assertTrue(e.hasDetails());
      System.out.println(e.getMessage());
    }
  }

  @Test
  public void validMessage() throws TestException {
    TradingSessionStatus message = new TradingSessionStatus();
    message.set(new TradingSessionID(TradingSessionID.DAY));
    message.set(new TradSesStatus(TradSesStatus.OPEN));
    MessageType messageType = repositoryAdapter.getMessage("TradingSessionStatus", "base");
    validator.validate(message, messageType);
  }

  /**
   * Conditionally required field test
   * @throws TestException expected
   * 
    <pre>
    <fixr:fieldRef id="567" name="TradSesStatusRejReason" added="FIX.4.4" presence="conditional">
                    <fixr:rule name="TradSesStatusRejReason" presence="required">
                        <fixr:when>TradSesStatus=^RequestRejected</fixr:when>
                    </fixr:rule>
    </pre>
   */
  @Test
  public void ruleViolation() throws TestException {
    TradingSessionStatus message = new TradingSessionStatus();
    message.set(new TradingSessionID(TradingSessionID.DAY));
    message.set(new TradSesStatus(TradSesStatus.REQUEST_REJECTED));
    MessageType messageType = repositoryAdapter.getMessage("TradingSessionStatus", "base");

    assertThrows(TestException.class, () -> {validator.validate(message, messageType);});
  }

}
