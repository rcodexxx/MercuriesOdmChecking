USE [UATODMDB]
GO

/****** Object:  Table [dbo].[nb_case_in]    Script Date: 2024/11/14 下午 05:18:27 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[nb_case_in](
	[trans_no] [varchar](50) NOT NULL,
	[policy_no] [varchar](50) NULL,
	[service_no] [varchar](50) NULL,
	[txn_seq] [varchar](50) NULL,
	[sequence] [varchar](50) NULL,
	[business] [varchar](50) NULL,
	[channel] [varchar](50) NULL,
	[station] [varchar](50) NULL,
	[swt_opt] [varchar](50) NULL,
	[prog_type] [varchar](50) NULL,
	[batch_date] [varchar](50) NULL,
	[process_user] [varchar](50) NULL,
	[keep_date_time] [datetime] NULL,
	[nb_json_in] [text] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

ALTER TABLE [dbo].[nb_case_in] ADD  CONSTRAINT [DF_nb_case_in_keep_date_time]  DEFAULT (getdate()) FOR [keep_date_time]
GO

