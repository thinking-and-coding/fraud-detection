import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import React, { forwardRef, useRef, useState } from "react";
import {
  faArrowRight,
  faCreditCard,
  faMoneyBill,
  faQuestionCircle,
  IconDefinition,
  faRocket,
} from "@fortawesome/free-solid-svg-icons";
import { Badge, Card, CardBody, CardHeader, Col } from "reactstrap";
import styled from "styled-components/macro";
import { Event } from "../interfaces";
import Slider from "react-rangeslider";
import { useLocalStorage, useUpdateEffect } from "react-use";
import { AutoSizer, List, ListRowRenderer } from "react-virtualized";
import SockJsClient from "react-stomp";
import "react-virtualized/styles.css";

// MSG
// beneficiaryId: 42694
// eventTime: 1565965071385
// payeeId: 20908
// paymentAmount: 13.54
// paymentType: "CRD"
// eventId: 5954524216210268000

export const paymentTypeMap: {
  [s: string]: IconDefinition;
} = {
  CRD: faCreditCard,
  CSH: faMoneyBill,
  undefined: faQuestionCircle,
};

const EventsCard = styled(Card)`
  width: 100%;
  height: 100%;
  border-left: 0 !important;

  .rangeslider__handle {
    &:focus {
      outline: 0;
    }
  }
`;

const EventsHeading = styled.div`
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid rgba(0, 0, 0, 0.125);
  font-weight: 500;
`;

export const Payment = styled.div`
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 40px;
  border-top: 1px solid rgba(0, 0, 0, 0.125);

  &.text-danger {
    border-top: 1px solid #dc3545;
    border-bottom: 1px solid #dc3545;
  }

  &.text-danger + & {
    border-top: 0;
  }

  &:first-of-type {
    border-top: none;
  }
`;

const FlexSpan = styled.span`
  display: inline-flex;
  align-items: center;
  width: 120px;
  flex-basis: 33%;
  flex: 1 1 auto;
`;

export const Payee = styled(FlexSpan)`
  justify-content: flex-start;
`;

export const EventName = styled(FlexSpan)`
  justify-content: center;
`;

export const Details = styled(FlexSpan)`
  justify-content: center;
`;

export const Beneficiary = styled(FlexSpan)`
  justify-content: flex-end;
`;

const EventsOverlay = styled.div`
  position: absolute;
  z-index: 10;
  background: rgba(0, 0, 0, 0.7);
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  color: white;
`;

const Rocket = styled.span`
  font-size: 500%;
  width: 100%;
`;

const getFakeValue = (value: number) => {
  return value <= 10 ? value : value <= 20 ? (value - 10) * 10 : (value - 20) * 100;
};

export const Events = React.memo(
  forwardRef<HTMLDivElement, {}>((props, ref) => {
    const list = useRef<List>(null);
    const [events, setEvents] = useState<Event[]>([]);
    const addEvent = (event: Event) => setEvents(state => [...state.slice(-33), event]);

    const [generatorSpeed, setGeneratorSpeed] = useLocalStorage("generatorSpeed", 1);
    const handleSliderChange = (val: number) => setGeneratorSpeed(val);

    useUpdateEffect(() => {
      fetch(`/api/generatorSpeed/${getFakeValue(generatorSpeed)}`);
    }, [generatorSpeed]);

    const renderRow: ListRowRenderer = ({ key, index, style }) => {
      const t = events[index];

      return (
        <Payment key={key} style={style} className="px-2">
          <Payee>{t.payeeId}</Payee>
          <EventName>{t.eventName}</EventName>
          <Details>
            <FontAwesomeIcon className="mx-1" icon={paymentTypeMap[t.paymentType]} />
            <Badge color="info">${parseFloat(t.paymentAmount.toString()).toFixed(2)}</Badge>
            <FontAwesomeIcon className="mx-1" icon={faArrowRight} />
          </Details>
          <Beneficiary>{t.beneficiaryId}</Beneficiary>
        </Payment>
      );
    };

    return (
      <>
        <SockJsClient url="/ws/backend" topics={["/topic/events"]} onMessage={addEvent} />
        <Col xs="2" className="d-flex flex-column px-0">
          <EventsCard innerRef={ref}>
            <CardHeader className="d-flex align-items-center py-0 justify-content-between">
              <div style={{ width: 160 }} className="mr-3 d-inline-block">
                <Slider
                  value={generatorSpeed}
                  onChange={handleSliderChange}
                  max={30}
                  min={0}
                  tooltip={false}
                  step={1}
                />
              </div>
              <span>{getFakeValue(generatorSpeed)}</span>
            </CardHeader>
            <CardBody className="p-0 mb-0" style={{ pointerEvents: "none" }}>
              <EventsOverlay hidden={generatorSpeed < 16}>
                <div>
                  <Rocket>
                    <FontAwesomeIcon icon={faRocket} />
                    {/* <span role="img" aria-label="rocket">
                      🚀
                    </span> */}
                  </Rocket>
                  <h2>Events per-second too high to render...</h2>
                </div>
              </EventsOverlay>
              <EventsHeading className="px-2 py-1">
                <span>Payer</span>
                <span>Event</span>
                <span>Amount</span>
                <span>Beneficiary</span>
              </EventsHeading>
              <AutoSizer>
                {({ height, width }) => (
                  <List
                    ref={list}
                    height={height}
                    width={width}
                    rowHeight={40}
                    rowCount={events.length - 1}
                    rowRenderer={renderRow}
                  />
                )}
              </AutoSizer>
            </CardBody>
          </EventsCard>
        </Col>
      </>
    );
  })
);
