import { library } from "@fortawesome/fontawesome-svg-core";
import {
  faArrowUp,
  faCalculator,
  faClock,
  faFont,
  faInfoCircle,
  faLaptopCode,
  faLayerGroup,
  IconDefinition,
} from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import Axios from "axios";
import { isArray } from "lodash/fp";
import React, { FC } from "react";

import { Badge, Button, CardBody, CardFooter, CardHeader, Table } from "reactstrap";
import styled from "styled-components/macro";
import { Alert, Strategy } from "../interfaces";
import { CenteredContainer } from "./CenteredContainer";
import { ScrollingCol } from "./App";
import { Line } from "app/utils/useLines";

library.add(faInfoCircle);

const badgeColorMap: {
  [s: string]: string;
} = {
  ACTIVE: "success",
  DELETE: "danger",
  PAUSE: "warning",
};

const iconMap: {
  [s: string]: IconDefinition;
} = {
  aggregateFieldName: faFont,
  aggregatorFunctionType: faCalculator,
  groupingKeyNames: faLayerGroup,
  limit: faArrowUp,
  limitOperatorType: faLaptopCode,
  windowMinutes: faClock,
  events: faFont
};

const seperator: {
  [s: string]: string;
} = {
  EQUAL: "to",
  GREATER: "than",
  GREATER_EQUAL: "than",
  LESS: "than",
  LESS_EQUAL: "than",
  NOT_EQUAL: "to",
};

const StrategyTitle = styled.div`
  display: flex;
  align-items: center;
`;

const StrategyTable = styled(Table)`
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

const fields = [
  "aggregatorFunctionType",
  "aggregateFieldName",
  "groupingKeyNames",
  "events",
  "limitOperatorType",
  "limit",
  "windowMinutes",
];

// const omitFields = omit(["strategyId", "strategyState"]);

const hasAlert = (alerts: Alert[], strategy: Strategy) => alerts.some(alert => alert.strategyId === strategy.id);

export const Strategies: FC<Props> = props => {
  const handleDelete = (id: number) => () => {
    Axios.delete(`/api/strategies/${id}`).then(props.clearStrategy(id));
  };

  const handleScroll = () => {
    props.strategyLines.forEach(line => line.line.position());
    props.alertLines.forEach(line => line.line.position());
  };

  const tooManyStrategies = props.strategies.length > 3;

  return (
    <ScrollingCol xs={{ size: 5, offset: 1 }} onScroll={handleScroll}>
      {props.strategies.map(strategy => {
        const payload = JSON.parse(strategy.strategyPayload);

        if (!payload) {
          return null;
        }

        return (
          <CenteredContainer
            ref={strategy.ref}
            key={strategy.id}
            tooManyItems={tooManyStrategies}
            style={{
              borderColor: hasAlert(props.alerts, strategy) ? "#dc3545" : undefined,
              borderWidth: hasAlert(props.alerts, strategy) ? 2 : 1,
            }}
          >
            <CardHeader className="d-flex justify-content-between align-items-center" style={{ padding: "0.3rem" }}>
              <StrategyTitle>
                <FontAwesomeIcon icon={faInfoCircle} fixedWidth={true} className="mr-2" />
                Strategy #{strategy.id}{" "}
                <Badge color={badgeColorMap[payload.strategyPayload]} className="ml-2">
                  {payload.strategyPayload}
                </Badge>
              </StrategyTitle>
              <Button size="sm" color="danger" outline={true} onClick={handleDelete(strategy.id)}>
                Delete
              </Button>
            </CardHeader>
            <CardBody className="p-0">
              <StrategyTable size="sm" bordered={true}>
                <tbody>
                  {fields.map(key => {
                    const field = payload[key];
                    return (
                      <tr key={key}>
                        <td style={{ width: 10 }}>
                          <FontAwesomeIcon icon={iconMap[key]} fixedWidth={true} />
                        </td>
                        <td style={{ width: 30 }}>{key}</td>
                        <td>{isArray(field) ? field.map(v => `"${v}"`).join(", ") : field}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </StrategyTable>
            </CardBody>
            <CardFooter style={{ padding: "0.3rem" }}>
              <em>{payload.aggregatorFunctionType}</em> of <em>{payload.aggregateFieldName}</em> aggregated by "
              <em>{payload.groupingKeyNames.join(", ")}</em>" is <em>{payload.limitOperatorType}</em>{" "}
              {seperator[payload.limitOperatorType]} <em>{payload.limit}</em> within an interval of{" "}
              <em>{payload.windowMinutes}</em> minutes.
            </CardFooter>
          </CenteredContainer>
        );
      })}
    </ScrollingCol>
  );
};

interface Props {
  alerts: Alert[];
  strategies: Strategy[];
  clearStrategy: (id: number) => () => void;
  strategyLines: Line[];
  alertLines: Line[];
}
