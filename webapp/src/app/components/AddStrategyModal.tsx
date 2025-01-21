import {
  faArrowUp,
  faCalculator,
  faClock,
  faFont,
  faInfoCircle,
  faLaptopCode,
  faLayerGroup,
} from "@fortawesome/free-solid-svg-icons";
import Axios from "axios";
import getFormData from "get-form-data";
import { isArray, pick } from "lodash/fp";
import React, { createRef, FC, FormEvent, useState, MouseEvent } from "react";
import CreatableSelect from "react-select/creatable";
import { Alert, Button, Input, Modal, ModalBody, ModalFooter, ModalHeader } from "reactstrap";
import { Strategy, StrategyPayload } from "../interfaces/";
import { FieldGroup } from "./FieldGroup";

const headers = { "Content-Type": "application/json" };

const pickFields = pick([
  "aggregateFieldName",
  "aggregatorFunctionType",
  "groupingKeyNames",
  "limit",
  "limitOperatorType",
  "strategiesState",
  "events",
  "windowMinutes",
]);

type ResponseError = {
  error: string;
  message: string;
} | null;

const sampleStrategies: {
  [n: number]: StrategyPayload;
} = {
  1: {
    aggregateFieldName: "metadata.totalFare",
    aggregatorFunctionType: "SUM",
    groupingKeyNames: ["accountUuid", "vtUuid"],
    limit: 200,
    limitOperatorType: "GREATER",
    windowMinutes: 30,
    strategyState: "ACTIVE",
    events: ["pay","refund"],
  },
   2: {
     aggregateFieldName: "metadata.amount",
     aggregatorFunctionType: "SUM",
     groupingKeyNames: ["vtUuid"],
     limit: 100,
     limitOperatorType: "GREATER_EQUAL",
     windowMinutes: 10,
     strategyState: "ACTIVE",
     events: ["pay"],
   },
  3: {
    aggregateFieldName: "COUNT_WITH_RESET_FLINK",
    aggregatorFunctionType: "SUM",
    groupingKeyNames: ["accountUuid"],
    limit: 100,
    limitOperatorType: "GREATER_EQUAL",
    windowMinutes: 60,
    strategyState: "ACTIVE",
    events: ["refund"],
  },

};

const groupingKeyNames = ["accountUuid", "vtUuid", "event"];
const aggregateKeywords = ["metadata.totalFare", "metadata.amount", "metadata.fee", "COUNT_FLINK", "COUNT_WITH_RESET_FLINK"];
const events = ["pay", "refund", "open", "close"];

const MySelect = React.memo(CreatableSelect);

export const AddStrategyModal: FC<Props> = props => {
  const [error, setError] = useState<ResponseError>(null);

  const handleClosed = () => {
    setError(null);
    props.onClosed();
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const data = pickFields(getFormData(e.target)) as StrategyPayload;
    data.groupingKeyNames = isArray(data.groupingKeyNames) ? data.groupingKeyNames : [data.groupingKeyNames];
    data.events = isArray(data.events) ? data.events : [data.events];

    const strategyPayload = JSON.stringify(data);
    const body = JSON.stringify({ strategyPayload: strategyPayload });

    setError(null);
    Axios.post<Strategy>("/api/strategies", body, { headers })
      .then(response => props.setStrategies(strategies => [...strategies, { ...response.data, ref: createRef<HTMLDivElement>() }]))
      .then(props.onClosed)
      .catch(setError);
  };

  const postSampleStrategy = (strategyId: number) => (e: MouseEvent) => {
    const strategyPayload = JSON.stringify(sampleStrategies[strategyId]);
    const body = JSON.stringify({ strategyPayload });

    Axios.post<Strategy>("/api/strategies", body, { headers })
      .then(response => props.setStrategies(strategies => [...strategies, { ...response.data, ref: createRef<HTMLDivElement>() }]))
      .then(props.onClosed)
      .catch(setError);
  };

  return (
    <Modal
      isOpen={props.isOpen}
      onClosed={handleClosed}
      toggle={props.toggle}
      backdropTransition={{ timeout: 75 }}
      modalTransition={{ timeout: 150 }}
      size="lg"
    >
      <form onSubmit={handleSubmit}>
        <ModalHeader toggle={props.toggle}>Add a new Strategy</ModalHeader>
        <ModalBody>
          {error && <Alert color="danger">{error.error + ": " + error.message}</Alert>}
          <FieldGroup label="strategyState" icon={faInfoCircle}>
            <Input type="select" name="strategyState" bsSize="sm">
              <option value="ACTIVE">ACTIVE</option>
              <option value="PAUSE">PAUSE</option>
              <option value="DELETE">DELETE</option>
            </Input>
          </FieldGroup>

          <FieldGroup label="events" icon={faInfoCircle}>
            <MySelect
                isMulti={true}
                name="events"
                className="react-select"
                classNamePrefix="react-select"
                options={events.map(k => ({ value: k, label: k }))}
            />
          </FieldGroup>

          <FieldGroup label="aggregatorFunctionType" icon={faCalculator}>
            <Input type="select" name="aggregatorFunctionType" bsSize="sm">
              <option value="SUM">SUM</option>
              <option value="AVG">AVG</option>
              <option value="MIN">MIN</option>
              <option value="MAX">MAX</option>
            </Input>
          </FieldGroup>

          <FieldGroup label="aggregateFieldName" icon={faFont}>
            <Input name="aggregateFieldName" type="select" bsSize="sm">
              {aggregateKeywords.map(k => (
                <option key={k} value={k}>
                  {k}
                </option>
              ))}
            </Input>
          </FieldGroup>

          <FieldGroup label="groupingKeyNames" icon={faLayerGroup}>
            <MySelect
              isMulti={true}
              name="groupingKeyNames"
              className="react-select"
              classNamePrefix="react-select"
              options={groupingKeyNames.map(k => ({ value: k, label: k }))}
            />
          </FieldGroup>

          <FieldGroup label="limitOperatorType" icon={faLaptopCode}>
            <Input type="select" name="limitOperatorType" bsSize="sm">
              <option value="EQUAL">EQUAL (=)</option>
              <option value="NOT_EQUAL">NOT_EQUAL (!=)</option>
              <option value="GREATER_EQUAL">GREATER_EQUAL (>=)</option>
              <option value="LESS_EQUAL">LESS_EQUAL ({"<="})</option>
              <option value="GREATER">GREATER (>)</option>
              <option value="LESS">LESS ({"<"})</option>
            </Input>
          </FieldGroup>
          <FieldGroup label="limit" icon={faArrowUp}>
            <Input name="limit" bsSize="sm" type="number" />
          </FieldGroup>
          <FieldGroup label="windowMinutes" icon={faClock}>
            <Input name="windowMinutes" bsSize="sm" type="number" />
          </FieldGroup>
        </ModalBody>
        <ModalFooter className="justify-content-between">
          <div>
            <Button color="secondary" onClick={postSampleStrategy(1)} size="sm" className="mr-2">
              Sample Strategy 1
            </Button>
            <Button color="secondary" onClick={postSampleStrategy(2)} size="sm" className="mr-2">
              Sample Strategy 2
            </Button>
            <Button color="secondary" onClick={postSampleStrategy(3)} size="sm" className="mr-2">
              Sample Strategy 3
            </Button>
          </div>
          <div>
            <Button color="secondary" onClick={handleClosed} size="sm" className="mr-2">
              Cancel
            </Button>
            <Button type="submit" color="primary" size="sm">
              Submit
            </Button>
          </div>
        </ModalFooter>
      </form>
    </Modal>
  );
};

interface Props {
  toggle: () => void;
  isOpen: boolean;
  onClosed: () => void;
  setStrategies: (fn: (strategies: Strategy[]) => Strategy[]) => void;
}
