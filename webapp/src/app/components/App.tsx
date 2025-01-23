import { Header, Alerts, Strategies, Events } from "app/components";
import { Strategy, Alert } from "app/interfaces";
// import { useLines } from "app/utils/useLines";
import Axios from "axios";
import React, { createRef, FC, useEffect, useRef, useState } from "react";
import { Col, Container, Row } from "reactstrap";
import styled from "styled-components/macro";
import SockJsClient from "react-stomp";
import uuid from "uuid/v4";
import LeaderLine from "leader-line";
import { intersectionWith, find } from "lodash/fp";

import "../assets/app.scss";
import { Line } from "app/utils/useLines";

// edit for strategy timeouts. (s * ms)
const STRATEGY_TIMEOUT = 5 * 1000;

const LayoutContainer = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;
  max-height: 100vh;
  height: 100vh;
  overflow: hidden;
`;

export const ScrollingCol = styled(Col)`
  overflow-y: scroll;
  max-height: 100%;
  display: flex;
  flex-direction: column;
`;

export const App: FC = () => {
  const [strategies, setStrategies] = useState<Strategy[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [strategyLines, setStrategyLines] = useState<Line[]>([]);
  const [alertLines, setAlertLines] = useState<Line[]>([]);

  const eventsRef = useRef<HTMLDivElement>(null);
  // const { handleScroll } = useLines(eventsRef, strategies, alerts);

  useEffect(() => {
    Axios.get<Strategy[]>("/api/strategies").then(response =>
      setStrategies(response.data.map(strategy => ({ ...strategy, ref: createRef<HTMLDivElement>() })))
    );
  }, []);

  useEffect(() => {
    const newLines = strategies.map(strategy => {
      try {
        return {
          line: new LeaderLine(eventsRef.current, strategy.ref.current, {
            dash: { animation: true },
            endSocket: "left",
            startSocket: "right",
          }),
          strategyId: strategy.id,
        };
      } catch (e) {
        return {
          line: {
            position: () => {},
            remove: () => {},
          },
          strategyId: strategy.id,
        };
      }
    });

    setStrategyLines(newLines);

    return () => newLines.forEach(line => line.line.remove());
  }, [strategies]);

  useEffect(() => {
    const alertingStrategies = intersectionWith((strategy, alert) => strategy.id === alert.strategyId, strategies, alerts).map(
        strategy => strategy.id
    );
    strategyLines.forEach(line => {
      try {
        line.line.color = alertingStrategies.includes(line.strategyId) ? "#dc3545" : "#ff7f50";
      } catch (e) {
        // nothing
      }
    });
  }, [strategies, alerts, strategyLines]);

  useEffect(() => {
    const newLines = alerts.map(alert => {
      const strategy = find(strategy => strategy.id === alert.strategyId, strategies);

      return {
        line: new LeaderLine(strategy!.ref.current, alert.ref.current, {
          color: "#fff",
          endPlugOutline: true,
          endSocket: "left",
          outline: true,
          outlineColor: "#dc3545",
          startSocket: "right",
        }),
        strategyId: strategy!.id,
      };
    });

    setAlertLines(newLines);

    return () => newLines.forEach(line => line.line.remove());
  }, [alerts, strategies]);

  const clearStrategy = (id: number) => () => setStrategies(strategies.filter(strategy => id !== strategy.id));

  const clearAlert = (id: number) => () => {
    setAlerts(state => {
      const newAlerts = [...state];
      newAlerts.splice(id, 1);
      return newAlerts;
    });
  };

  const handleMessage = (alert: Alert) => {
    const alertId = uuid();
    const newAlert = {
      ...alert,
      alertId,
      ref: createRef<HTMLDivElement>(),
      timeout: setTimeout(() => setAlerts(state => state.filter(a => a.alertId !== alertId)), STRATEGY_TIMEOUT),
    };

    setAlerts((state: Alert[]) => {
      const filteredState = state.filter(a => a.strategyId !== alert.strategyId);
      return [...filteredState, newAlert].sort((a, b) => (a.strategyId > b.strategyId ? 1 : -1));
    });
  };

  const handleLatencyMessage = (latency: string) => {
    // tslint:disable-next-line: no-console
    console.info(latency);
  };

  return (
    <>
      <SockJsClient url="/ws/backend" topics={["/topic/alerts"]} onMessage={handleMessage} />
      <SockJsClient url="/ws/backend" topics={["/topic/latency"]} onMessage={handleLatencyMessage} />
      <LayoutContainer>
        <Header setStrategies={setStrategies} />
        <Container fluid={true} className="flex-grow-1 d-flex w-100 flex-column overflow-hidden">
          <Row className="flex-grow-1 overflow-hidden">
            <Events ref={eventsRef} />
            <Strategies clearStrategy={clearStrategy} strategies={strategies} alerts={alerts} strategyLines={strategyLines} alertLines={alertLines} />
            <Alerts alerts={alerts} clearAlert={clearAlert} lines={alertLines} />
          </Row>
        </Container>
      </LayoutContainer>
    </>
  );
};
