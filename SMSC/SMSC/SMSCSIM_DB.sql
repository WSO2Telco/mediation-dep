-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               5.6.17 - MySQL Community Server (GPL)
-- Server OS:                    Win64
-- HeidiSQL Version:             9.1.0.4867
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

-- Dumping database structure for smscsim
CREATE DATABASE IF NOT EXISTS `smscsim` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `smscsim`;


-- Dumping structure for table smscsim.delivery_notif
CREATE TABLE IF NOT EXISTS `delivery_notif` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `address` varchar(50) DEFAULT NULL,
  `body` varchar(2000) NOT NULL,
  `recieved_at` timestamp NOT NULL,
  `delivered` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table smscsim.delivery_notif: ~0 rows (approximately)
/*!40000 ALTER TABLE `delivery_notif` DISABLE KEYS */;
/*!40000 ALTER TABLE `delivery_notif` ENABLE KEYS */;


-- Dumping structure for table smscsim.hub_req_resp_log
CREATE TABLE IF NOT EXISTS `hub_req_resp_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sme_id` int(11) NOT NULL,
  `ref_num` varchar(50) NOT NULL DEFAULT '0',
  `type_request` tinyint(1) NOT NULL,
  `pdu` int(11) NOT NULL,
  `sender_addr` varchar(50) DEFAULT NULL,
  `body` varchar(2000) DEFAULT NULL,
  `logged_at` timestamp NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_hub_req_resp_log_sme` (`sme_id`),
  KEY `FK_hub_req_resp_log_pdu_2` (`pdu`),
  CONSTRAINT `FK_hub_req_resp_log_pdu` FOREIGN KEY (`pdu`) REFERENCES `pdu` (`id`),
  CONSTRAINT `FK_hub_req_resp_log_pdu_2` FOREIGN KEY (`pdu`) REFERENCES `pdu` (`id`),
  CONSTRAINT `FK_hub_req_resp_log_sme` FOREIGN KEY (`sme_id`) REFERENCES `sme` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table smscsim.hub_req_resp_log: ~0 rows (approximately)
/*!40000 ALTER TABLE `hub_req_resp_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `hub_req_resp_log` ENABLE KEYS */;


-- Dumping structure for table smscsim.mo_sms
CREATE TABLE IF NOT EXISTS `mo_sms` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ref_num` varchar(50) DEFAULT NULL,
  `body` varchar(2000) NOT NULL,
  `recieved_at` timestamp NOT NULL,
  `delivered` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table smscsim.mo_sms: ~0 rows (approximately)
/*!40000 ALTER TABLE `mo_sms` DISABLE KEYS */;
/*!40000 ALTER TABLE `mo_sms` ENABLE KEYS */;


-- Dumping structure for table smscsim.pdu
CREATE TABLE IF NOT EXISTS `pdu` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=latin1;

-- Dumping data for table smscsim.pdu: ~4 rows (approximately)
/*!40000 ALTER TABLE `pdu` DISABLE KEYS */;
INSERT INTO `pdu` (`id`, `name`) VALUES
	(1, 'submit_sm'),
	(2, 'submit_sm_multi'),
	(3, 'query_sm'),
	(4, 'deliver_sm');
/*!40000 ALTER TABLE `pdu` ENABLE KEYS */;


-- Dumping structure for table smscsim.sme
CREATE TABLE IF NOT EXISTS `sme` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `code` varchar(20) DEFAULT NULL,
  `subscribed_to_delivery_notif` tinyint(1) NOT NULL DEFAULT '0',
  `subscribed_to_mo_sms` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;

-- Dumping data for table smscsim.sme: ~0 rows (approximately)
/*!40000 ALTER TABLE `sme` DISABLE KEYS */;
INSERT INTO `sme` (`id`, `name`, `code`, `subscribed_to_delivery_notif`, `subscribed_to_mo_sms`) VALUES
	(1, 'pavel', 'pavel', 0, 0);
/*!40000 ALTER TABLE `sme` ENABLE KEYS */;


-- Dumping structure for table smscsim.smse_req_resp_log
CREATE TABLE IF NOT EXISTS `smse_req_resp_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sme_id` int(11) NOT NULL,
  `ref_num` varchar(50) NOT NULL DEFAULT '0',
  `type_request` tinyint(1) NOT NULL,
  `pdu` int(11) NOT NULL,
  `sender_addr` varchar(50) DEFAULT NULL,
  `need_delivery_report` tinyint(4) NOT NULL DEFAULT '0',
  `body` varchar(2000) DEFAULT NULL,
  `sar_seq_num` int(11) NOT NULL DEFAULT '0',
  `sar_ref_num` int(11) NOT NULL DEFAULT '0',
  `sar_total_segmants` int(11) NOT NULL DEFAULT '0',
  `logged_at` timestamp NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_smse_req_resp_log_sme` (`sme_id`),
  KEY `FK_smse_req_resp_log_pdu` (`pdu`),
  CONSTRAINT `FK_smse_req_resp_log_pdu` FOREIGN KEY (`pdu`) REFERENCES `pdu` (`id`),
  CONSTRAINT `FK_smse_req_resp_log_sme` FOREIGN KEY (`sme_id`) REFERENCES `sme` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table smscsim.smse_req_resp_log: ~0 rows (approximately)
/*!40000 ALTER TABLE `smse_req_resp_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `smse_req_resp_log` ENABLE KEYS */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
