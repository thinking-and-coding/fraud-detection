import React, { FC } from "react";
import { Button, CardBody, CardHeader, Table, CardFooter, Badge } from "reactstrap";
import styled from "styled-components/macro";
import { faArrowLeft, faArrowRight } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";

import { Alert } from "../interfaces";
import { CenteredContainer } from "./CenteredContainer";
import { ScrollingCol } from "./App";
import { EventInfo, AccountUuid, EventName, Metadata, VtUuid } from "./Events";
import { Line } from "app/utils/useLines";

const AlertTable = styled(Table)`
  && {
    width: calc(100% + 1px);
    border: 0;
    margin: 0;

    td {
      vertical-align: middle !important;

      &:first-child {
        border-left: 0;
      }

      &:last-child {
        border-right: 0;
      }
    }

    tr:first-child {
      td {
        border-top: 0;
      }
    }
  }
`;

export const Alerts: FC<Props> = props => {
  const tooManyAlerts = props.alerts.length > 4;

  const handleScroll = () => {
    props.lines.forEach(line => line.line.position());
  };

  return (
    <ScrollingCol xs={{ size: 3, offset: 1 }} onScroll={handleScroll}>
      {props.alerts.map((alert, idx) => {
        const t = alert.triggeringEvent;
        return (
          <CenteredContainer
            key={idx}
            className="w-100"
            ref={alert.ref}
            tooManyItems={tooManyAlerts}
            style={{ borderColor: "#ffc107", borderWidth: 2 }}
          >
            <CardHeader>
              Alert
              <Button size="sm" color="primary" onClick={props.clearAlert(idx)} className="ml-3">
                Clear Alert
              </Button>
            </CardHeader>
            <CardBody className="p-0">
              <AlertTable size="sm" bordered={true}>
                <tbody>
                  <tr>
                    <td>Event</td>
                    <td>{alert.triggeringEvent.event}</td>
                  </tr>
                  <tr>
                    <td colSpan={2} className="p-0" style={{ borderBottomWidth: 3 }}>
                      <EventInfo className="px-2">
                        {t.accountUuid}-{t.vtUuid}-{JSON.stringify(t.metadata)}
                      </EventInfo>
                    </td>
                  </tr>
                  <tr>
                    <td>Strategy</td>
                    <td>{alert.strategyId}</td>
                  </tr>
                  <tr>
                    <td>Amount</td>
                    <td>{alert.triggeringValue}</td>
                  </tr>
                  <tr>
                    <td>Of</td>
                    <td>{alert.violatedStrategy.aggregateFieldName}</td>
                  </tr>
                </tbody>
              </AlertTable>
            </CardBody>
            <CardFooter style={{ padding: "0.3rem" }}>
              Alert for Strategy <em>{alert.strategyId}</em> caused by Event{" "}
              <em>{alert.triggeringEvent.id}</em> with Amount <em>{alert.triggeringValue}</em> of{" "}
              <em>{alert.violatedStrategy.aggregateFieldName}</em>.
            </CardFooter>
          </CenteredContainer>
        );
      })}
    </ScrollingCol>
  );
};

interface Props {
  alerts: Alert[];
  clearAlert: any;
  lines: Line[];
  // handleScroll: () => void;
}
