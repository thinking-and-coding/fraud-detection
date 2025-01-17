import logoImage from "app/assets/flink_squirrel_200_color.png";
import React, { FC, useState, Dispatch, SetStateAction } from "react";
import { Button, ButtonGroup, Col, Navbar, NavbarBrand } from "reactstrap";
import styled from "styled-components/macro";
import { AddStrategyModal } from "./AddStrategyModal";
import { Strategy } from "app/interfaces";

const AppNavbar = styled(Navbar)`
  && {
    z-index: 1;
    justify-content: flex-start;
    padding: 0;
  }
`;

const Logo = styled.img`
  max-height: 40px;
`;

const EventsCol = styled(Col)`
  border-right: 1px solid rgba(255, 255, 255, 0.125);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5em 15px;
`;

export const Header: FC<Props> = props => {
  const [modalOpen, setModalOpen] = useState(false);
  const openStrategyModal = () => setModalOpen(true);
  const closeStrategyModal = () => setModalOpen(false);
  const toggleStrategyModal = () => setModalOpen(state => !state);

  const startEvents = () => fetch("/api/startEventsGeneration").then();
  const stopEvents = () => fetch("/api/stopEventsGeneration").then();

  const syncStrategies = () => fetch("/api/syncStrategies").then();
  const clearState = () => fetch("/api/clearState").then();
  const pushToFlink = () => fetch("/api/strategies/pushToFlink").then();

  return (
    <>
      <AppNavbar color="dark" dark={true}>
        <EventsCol xs="2">
          <NavbarBrand tag="div">Live Events</NavbarBrand>
          <ButtonGroup size="sm">
            <Button color="success" onClick={startEvents}>
              Start
            </Button>
            <Button color="danger" onClick={stopEvents}>
              Stop
            </Button>
          </ButtonGroup>
        </EventsCol>

        <Col xs={{ size: 5, offset: 1 }}>
          <Button size="sm" color="primary" onClick={openStrategyModal}>
            Add Strategy
          </Button>

          <Button size="sm" color="warning" onClick={syncStrategies}>
            Sync Strategies
          </Button>

          <Button size="sm" color="warning" onClick={clearState}>
            Clear State
          </Button>

          <Button size="sm" color="warning" onClick={pushToFlink}>
            Push to Flink
          </Button>
        </Col>

        <Col xs={{ size: 3, offset: 1 }} className="justify-content-end d-flex align-items-center">
          <NavbarBrand tag="div">Apache Flink - Fraud Detection Demo</NavbarBrand>
          <Logo src={logoImage} title="Apache Flink" />
        </Col>
      </AppNavbar>
      <AddStrategyModal isOpen={modalOpen} toggle={toggleStrategyModal} onClosed={closeStrategyModal} setStrategies={props.setStrategies} />
    </>
  );
};

interface Props {
  setStrategies: Dispatch<SetStateAction<Strategy[]>>;
}
